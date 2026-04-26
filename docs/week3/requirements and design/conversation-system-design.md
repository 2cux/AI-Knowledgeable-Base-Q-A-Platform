# 第三周会话系统设计

## 1. 设计目标

在不推翻现有问答主链路和 `chat_record` 结构的前提下，引入最小可用的 `conversation/message` 模型，使项目从“只有 conversationId 轻量串联”升级为真正可维护的会话系统基础版。

## 2. 当前实现基线

当前实际情况：

- 数据库在初始化脚本中已经存在 `conversation` 和 `message` 表。
- 当前代码并未真正围绕这两张表形成实体、Mapper、Service、Controller 业务闭环。
- 问答历史上下文完全依赖 `chat_record`，通过 `conversationId` 查询最近几条记录。

这意味着第三周的最佳策略不是重新设计数据库，而是**补齐业务层，实现增量启用**。

## 3. 设计原则

- 不推翻现有 `chat_record`。
- 尽量复用已存在的 `conversation/message` 表结构。
- 对现有 `chat/ask` 改动最小。
- 优先保障现有管理端日志、问答记录、反馈链路不受影响。

## 4. 数据模型设计

## 4.1 conversation 定位

建议继续使用 `conversation` 表承载：

- 会话主键 `id`
- `user_id`
- `knowledge_base_id`
- `title`
- `created_at`
- `updated_at`

### 第三周建议补充

如当前表结构足够，则不强制改表。

如需增强，优先考虑以下字段，但第三周可选：

- `conversation_uid` 或直接将现有业务 `conversationId` 映射为外部可见 ID。
- `last_active_at`
- `status`

第三周如果不改表，也可先用：

- `conversation.id` 作为内部主键
- `chat_record.conversation_id` 继续作为外部业务 ID

但必须在实现层建立稳定映射规则。

## 4.2 message 定位

建议使用 `message` 表保存会话视图层消息：

- `conversation_id`
- `role`
- `content`
- `message_type`
- `quote_json`
- `created_at`

### 消息类型建议

- `QUESTION`
- `ANSWER`

### role 建议

- `USER`
- `ASSISTANT`

## 4.3 chat_record 与 conversation/message 的关系

建议采用“双轨模型”：

- `chat_record`：问答事实记录、运营日志、反馈与检索追溯基础。
- `conversation/message`：对话视图模型、上下文载体。

### 一次问答的写入关系

```text
一次 ask
  -> 写入一条 chat_record
  -> 写入 / 更新 conversation
  -> 写入两条 message
     - USER / QUESTION
     - ASSISTANT / ANSWER
```

这样可以避免把 `chat_record` 强行改造成消息表。

## 5. 兼容现有 conversationId 的演进方案

## 5.1 当前问题

当前 `conversationId` 是字符串，由 `chat_record` 使用，服务层直接按其串联历史。

## 5.2 第三周建议

采用“外部字符串 ID + 内部会话记录”双层映射方案。

### 方案 A

- 在 `conversation` 表新增 `conversation_uid`。
- `chat_record.conversation_id` 与 `conversation.conversation_uid` 对齐。

这是最清晰的方案，但需要改表。

### 方案 B

- 维持 `chat_record.conversation_id` 不变。
- 新建会话时，把 `conversationId` 作为会话业务唯一键在服务层维护。
- 如当前 `conversation` 表没有字段承载业务 UID，则第三周建议优先补字段。

### 建议结论

第三周推荐采用 **方案 A**：

- 对现有主链路兼容最好。
- 避免后续服务层做不透明映射。
- 更利于前后端明确使用统一会话标识。

## 6. 上下文加载策略设计

## 6.1 策略目标

把当前上下文加载从 `ChatService` 中抽离为独立规则。

## 6.2 建议规则

- 仅加载当前用户、当前知识库、当前会话的历史消息。
- 只加载最近 N 轮用户/助手消息对。
- 单条消息长度上限做截断。
- 组装 Prompt 时优先读取 message，不再直接依赖 `chat_record`。

## 6.3 第三周兼容策略

如 message 尚未覆盖所有历史数据，可采用：

- 新问答优先写 message。
- 老历史仍回退查 `chat_record`。

即：

```text
优先 message
若当前会话 message 不足
  -> 回退读取 chat_record
```

这样能保证演进期间不中断现有能力。

## 7. 接口设计建议

## 7.1 会话列表接口

建议新增：

- `GET /api/chat/conversations`

支持参数：

- `knowledgeBaseId` 可选
- `pageNum`
- `pageSize`

返回：

- `conversationId`
- `knowledgeBaseId`
- `title`
- `messageCount`
- `lastQuestion`
- `lastAnswerPreview`
- `lastActiveAt`
- `createdAt`

## 7.2 会话详情接口

建议新增：

- `GET /api/chat/conversations/{conversationId}`

返回：

- 会话基础信息
- 消息列表

每条消息建议包含：

- `messageId`
- `role`
- `messageType`
- `content`
- `citations`
- `createdAt`

## 7.3 与现有接口关系

- `POST /api/chat/ask` 保持不变。
- `GET /api/chat/records` 继续保留，面向问答记录视角。
- 新增会话接口，面向对话视角。

## 8. 写入流程设计

第三周建议在当前问答成功落库后，追加以下逻辑：

```text
1. 校验或创建 conversation
2. 写入 user question message
3. 写入 assistant answer message
4. 更新 conversation 最新活跃时间
```

### 注意事项

- 如果 `chat_record` 落库失败，不应写入 message。
- 如果 message 写入失败，需要评估是否回滚整个会话写入；第三周建议与 `chat_record` 放在同一事务内。

## 9. 标题生成设计

第三周建议采用简单标题策略：

- 使用首个问题前若干字符作为标题。
- 最长 50-80 字符。
- 不依赖 LLM 自动生成标题。

优势：

- 低成本。
- 结果稳定。
- 不增加额外模型调用。

## 10. 历史数据兼容策略

### 10.1 老数据现状

- 历史 `chat_record` 已有 `conversation_id`。
- 历史没有 `conversation/message` 业务闭环。

### 10.2 第三周兼容建议

推荐策略：

- 第三周新增能力只对新产生的问答强制写入 `conversation/message`。
- 对历史数据允许“延迟补建”。
- 如有必要，可增加离线补数脚本，但第三周不强制完成。

## 11. 风险与注意事项

- 若会话主键和外部 `conversationId` 映射不清晰，会导致后续接口混乱。
- 若直接把 `chat_record` 当消息表使用，会继续放大会话模型不清晰的问题。
- 若一次问答涉及多表写入，事务边界需要明确，避免会话数据与问答事实数据不一致。

## 12. 第三周完成标准

- `conversation/message` 业务闭环建立。
- 新问答能够同时形成 `chat_record` 与 `conversation/message` 双轨数据。
- 会话列表和会话详情接口可用。
- 上下文加载规则清晰，不再只是问答服务中的临时逻辑。
