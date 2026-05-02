# 会话系统总结

## 1. 为什么需要会话系统

单次 RAG 问答只能解决“一问一答”。真实问答场景中，用户经常会追问“那具体怎么配置？”、“这个和上一个有什么区别？”。如果系统没有会话，就无法知道追问和前文之间的关系。

因此第三周引入 `conversation` 和 `message`，用于承载多轮上下文、会话列表、会话详情和后续对话体验。

## 2. chat_record、conversation、message 的关系

- `chat_record`：一次问答的审计记录，重点是问题、答案、命中状态、检索数量、引用、创建时间。
- `conversation`：一组多轮问答的会话聚合，重点是会话标题、消息数、最后问题、最后回答预览、最近活跃时间。
- `message`：会话中的单条消息，区分 user / assistant，适合做多轮上下文加载。

三者不是互相替代关系：

- `chat_record` 适合追溯和运营统计。
- `message` 适合上下文拼接。
- `conversation` 适合会话列表和会话入口。

## 3. conversation_uid / conversationId 的设计

数据库实体中使用 `conversationUid` 作为业务会话 ID，对外接口使用 `conversationId`。

这样做的好处是：

- 不直接暴露数据库自增主键。
- 方便前端在后续追问时传入同一个会话 ID。
- 未来可以调整内部表结构，而不影响外部接口。

设计注意点：

- `conversationId` 应限制长度，当前 `ChatAskRequest` 限制最大 64 字符。
- 查询会话详情时必须校验当前用户权限。
- 对外 ID 不应包含可猜测的敏感信息。

## 4. chat/ask 如何写入双轨数据

`POST /api/chat/ask` 的写入逻辑大致为：

1. 根据当前用户和知识库确认权限。
2. 根据请求中的 `conversationId` 创建或复用会话。
3. 加载最近消息作为上下文。
4. 执行 RAG 检索和答案生成。
5. 在一个事务中写入 `chat_record`。
6. 写入 user message。
7. 写入 assistant message，并关联 `chatRecordId`。
8. 更新 conversation 的最后问题、回答预览、消息数和活跃时间。

这形成了“审计记录 + 会话消息”的双轨存储。

## 5. 会话列表接口

接口：`GET /api/chat/conversations`

支持参数：

- `knowledgeBaseId`：可选，筛选某个知识库下的会话。
- `page`：默认 1。
- `size`：默认 10，最大 100。

返回字段包括 `conversationId`、`title`、`knowledgeBaseId`、`messageCount`、`lastQuestion`、`lastAnswerPreview`、`lastActiveAt`、`createdAt`。

## 6. 会话详情接口

接口：`GET /api/chat/conversations/{conversationId}`

返回会话基本信息和消息列表，消息中包含：

- `messageId`
- `role`
- `content`
- `citations`
- `chatRecordId`
- `createdAt`

这使前端可以展示完整多轮问答，并跳转到具体问答记录详情。

## 7. 多轮上下文加载策略

当前 `ConversationContextLoaderImpl` 的策略是：

- 最多加载 10 条上下文消息。
- 总上下文文本最多约 4000 字符。
- 单条消息最多截断到约 600 字符。

这是一种 MVP 级保护策略，避免把整个历史会话无限拼入 prompt，导致成本和上下文长度失控。

## 8. 与 RAG 检索的关系

当前系统仍以“当前问题检索知识库”为主，会话上下文主要用于辅助 LLM 理解追问语境。

也就是说：

- 检索仍围绕用户本次问题进行。
- 生成答案时会把有效 chunk 和会话上下文一起放入 prompt。
- 知识依据仍应来自 citations 对应的 chunk，而不是纯聊天记忆。

## 9. 当前边界

- 多轮上下文是简单截断策略，没有做会话摘要。
- 检索 query 未必充分融合历史上下文，追问过短时可能影响召回。
- 没有会话删除 / 重命名接口，需以实际代码为准。
- 没有复杂前端会话管理。

## 10. 后续可优化方向

- 对追问进行 query rewrite，把上下文补全到检索 query。
- 增加会话摘要，减少长会话 token 占用。
- 增加会话删除、重命名、置顶。
- 将 message 和 chat_record 的一致性增加更多测试。
- 对 citations 和 message 的展示做更友好的前端联动。
