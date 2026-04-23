# 当前核心数据模型与状态流转说明

## 1. 文档定位

本文档用于说明当前 AI 知识库问答平台已经落地的核心数据模型、表关系和关键状态流转。

当前项目已经形成 MVP 级 RAG 后端骨架，主要数据闭环围绕以下链路展开：

```text
user
  -> knowledge_base
    -> document
      -> task_record
      -> document_chunk
        -> chunk_embedding
    -> chat_record
      -> chat_feedback
```

此外，数据库迁移中已经预留 `conversation` 和 `message` 表，但当前业务代码尚未形成独立会话/消息闭环。当前多轮能力主要依赖 `chat_record.conversation_id`。

## 2. 当前核心表及其职责

| 表名 | 当前职责 | 当前完成度 |
| --- | --- | --- |
| `user` | 保存用户账号、密码、邮箱、角色、状态 | 已支撑注册、登录、管理员校验 |
| `knowledge_base` | 保存知识库基础信息和所属用户 | 已支撑创建、列表、详情和数据隔离 |
| `document` | 保存文档元数据、所属知识库、上传人、解析状态 | 已支撑元数据上传、查询、切片状态 |
| `task_record` | 保存文档解析等后台任务的任务类型、业务对象、状态、错误信息 | 已支撑解析任务记录，第二周需补执行闭环 |
| `document_chunk` | 保存文档切片后的文本片段 | 已支撑切片入库和检索候选来源 |
| `chunk_embedding` | 保存 chunk 向量化状态、模型名、vectorId、失败原因 | 已支撑 mock embedding 状态，第二周需接真实 embedding/向量库 |
| `chat_record` | 保存一次问答的问题、答案、命中状态、引用来源、conversationId | 已支撑问答记录、未命中问题、轻量多轮 |
| `chat_feedback` | 保存用户对问答记录的 LIKE/DISLIKE 反馈 | 已支撑用户反馈和防重复 |
| `conversation` | 预留会话表 | 已建表，当前业务未接入 |
| `message` | 预留会话消息表 | 已建表，当前业务未接入 |

## 3. 表之间的关系

### 3.1 用户与知识库

```text
user.id 1 -> N knowledge_base.owner_id
```

说明：

- 一个用户可以拥有多个知识库。
- 当前普通用户查询知识库时按 `owner_id = 当前用户 ID` 隔离。
- 管理员权限不是通过知识库关系判断，而是通过 `user.role = ADMIN` 和 `user.status = 1` 判断。

### 3.2 知识库与文档

```text
knowledge_base.id 1 -> N document.knowledge_base_id
user.id 1 -> N document.created_by
```

说明：

- 文档归属于一个知识库。
- 文档同时记录上传人 `created_by`。
- 当前文档权限判断主要通过 document -> knowledge_base -> owner_id 完成。

### 3.3 文档与切片

```text
document.id 1 -> N document_chunk.document_id
knowledge_base.id 1 -> N document_chunk.knowledge_base_id
```

说明：

- 一个文档可以生成多个 chunk。
- chunk 冗余保存 `knowledge_base_id`，便于按知识库过滤检索。
- 当前重复处理文档时，会先删除旧 chunk，再重建 chunk。

### 3.4 文档与任务

```text
document.id 1 -> N task_record.biz_id
task_record.biz_type = DOCUMENT
task_record.task_type = DOCUMENT_PARSE
```

说明：

- `task_record` 使用通用业务关联模型，不直接外键到 document。
- 当前已创建解析任务记录，但第二周需要补齐任务执行和状态流转。
- 后续 embedding、索引构建也可以复用 `task_record`，但需要明确 `task_type`。

### 3.5 切片与向量化状态

```text
document_chunk.id 1 -> 1 chunk_embedding.chunk_id
document.id 1 -> N chunk_embedding.document_id
knowledge_base.id 1 -> N chunk_embedding.knowledge_base_id
```

说明：

- 当前 `chunk_embedding.chunk_id` 有唯一约束，一个 chunk 对应一条向量化状态记录。
- `chunk_embedding` 保存状态和 vectorId，不保存完整向量。
- 当前 mock 检索会从 `chunk_embedding` 找到 SUCCESS 的 chunk，再在 Java 内计算 mock 相似度。

### 3.6 知识库与问答记录

```text
knowledge_base.id 1 -> N chat_record.knowledge_base_id
user.id 1 -> N chat_record.user_id
```

说明：

- 每次问答都归属于一个用户和一个知识库。
- `matched=false` 的 chat_record 被管理端未命中问题查询复用。
- `citations_json` 保存问答引用来源。

### 3.7 问答记录与反馈

```text
chat_record.id 1 -> N chat_feedback.chat_record_id
user.id 1 -> N chat_feedback.user_id
```

说明：

- 一个问答记录可以有多个用户反馈，但当前用户端只允许“当前用户反馈自己的问答记录”。
- `chat_feedback` 有唯一约束 `chat_record_id + user_id`，同一用户对同一问答记录只能反馈一次。

### 3.8 conversationId 与问答记录

```text
chat_record.conversation_id
  + chat_record.user_id
  + chat_record.knowledge_base_id
  -> 轻量会话上下文查询
```

说明：

- 当前没有使用 `conversation` 表。
- 当前没有使用 `message` 表。
- 多轮上下文通过查询同一 `conversation_id` 下最近若干条 `chat_record` 实现。

## 4. 每张表的关键字段说明

### 4.1 user

职责：

- 保存登录账号、密码凭证和基础角色状态。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 用户主键 |
| `username` | 登录用户名，唯一 |
| `password_hash` | BCrypt 加密后的密码 |
| `email` | 邮箱 |
| `nickname` | 昵称，目前注册流程未显式设置 |
| `role` | 角色，当前用于 ADMIN 判断 |
| `status` | 用户状态，1 表示启用，0 表示禁用 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

当前注意点：

- 注册逻辑会写入 username、password_hash、email。
- 登录当前主要校验用户名和密码，第二周建议补充 status 校验。
- 管理端权限依赖 role/status。

### 4.2 knowledge_base

职责：

- 保存知识库基础信息和所属用户。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 知识库主键 |
| `name` | 知识库名称 |
| `description` | 知识库描述 |
| `owner_id` | 所属用户 ID |
| `status` | 状态，当前 1 表示启用 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

当前注意点：

- 当前只支持创建、列表、详情。
- 删除、禁用、更新尚未形成闭环。

### 4.3 document

职责：

- 保存文档元数据和解析状态。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 文档主键 |
| `knowledge_base_id` | 所属知识库 |
| `file_name` | 原始文件名 |
| `file_type` | 文件类型 |
| `file_size` | 文件大小 |
| `storage_path` | 文件存储路径。当前可为占位路径，第二周应升级为真实路径 |
| `parse_status` | 文档解析/处理状态 |
| `created_by` | 上传用户 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

当前注意点：

- 当前 `/api/documents/upload` 只保存元数据，不接收真实文件。
- 第二周真实文件上传后，`storage_path` 应指向后端可读取的真实文件。
- 当前失败原因不在 document 表中，建议通过 task_record.error_message 记录。

### 4.4 task_record

职责：

- 保存后台任务或业务处理任务的状态。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 任务主键 |
| `task_type` | 任务类型，例如 DOCUMENT_PARSE |
| `biz_type` | 业务类型，例如 DOCUMENT |
| `biz_id` | 业务对象 ID，例如 document.id |
| `status` | 任务状态 |
| `error_message` | 失败原因 |
| `retry_count` | 重试次数 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

当前注意点：

- 当前只在 parse 接口中创建 DOCUMENT_PARSE 任务。
- 当前任务没有 userId，权限查询时必须通过 biz_id 关联 document 再校验知识库归属。
- 第二周应补齐 `PENDING -> RUNNING -> SUCCESS/FAILED`。

### 4.5 document_chunk

职责：

- 保存文档切片结果，是 embedding 和检索的基础数据。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | chunk 主键 |
| `document_id` | 所属文档 |
| `knowledge_base_id` | 所属知识库 |
| `chunk_index` | 文档内切片顺序，从 0 开始 |
| `content` | chunk 文本内容 |
| `token_count` | 当前用字符数近似 token 数 |
| `created_at` | 创建时间 |

当前注意点：

- chunk 冗余 `knowledge_base_id`，有利于检索过滤。
- 重新切片会删除旧 chunk 并重建。
- `chunk_embedding.chunk_id` 对 `document_chunk.id` 有级联删除约束。

### 4.6 chunk_embedding

职责：

- 保存每个 chunk 的向量化状态和向量库标识。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `document_id` | 文档 ID |
| `knowledge_base_id` | 知识库 ID |
| `chunk_id` | chunk ID，唯一 |
| `embedding_model` | embedding 模型 |
| `vector_id` | 向量库中的向量 ID |
| `status` | 向量化状态 |
| `embedding_error` | 失败原因 |
| `embedded_at` | 向量化完成时间 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

当前注意点：

- 当前 vectorId 由 mock 实现生成。
- 当前不保存完整向量。
- 第二周真实向量库接入后，vectorId 应对应真实向量索引 ID 或外部存储主键。

### 4.7 chat_record

职责：

- 保存一次问答的完整业务记录。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 问答记录主键 |
| `user_id` | 提问用户 |
| `knowledge_base_id` | 问答所属知识库 |
| `conversation_id` | 轻量会话 ID |
| `question` | 用户问题 |
| `answer` | 生成答案 |
| `matched` | 是否检索命中 |
| `retrieved_chunk_count` | 命中 chunk 数量 |
| `top_k` | 本次检索 topK |
| `citations_json` | 引用来源 JSON |
| `created_at` | 创建时间 |

当前注意点：

- `matched=false` 被管理端未命中问题查询复用。
- 当前没有保存 answerMode、retrievalMode、model，如第二周要区分 mock/real，可考虑新增字段或在响应/日志中标注。
- 用户问答详情当前应补充返回 citations 和 conversationId。

### 4.8 chat_feedback

职责：

- 保存用户对问答结果的反馈。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 反馈主键 |
| `chat_record_id` | 被反馈的问答记录 |
| `user_id` | 反馈用户 |
| `feedback_type` | LIKE 或 DISLIKE |
| `comment` | 反馈备注 |
| `created_at` | 创建时间 |

当前注意点：

- 通过唯一约束限制同一用户对同一问答记录重复反馈。
- 当前用户只能反馈自己的问答记录。

### 4.9 conversation

职责：

- 预留会话表。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 会话主键 |
| `user_id` | 会话所属用户 |
| `knowledge_base_id` | 会话所属知识库 |
| `title` | 会话标题 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

当前注意点：

- 当前没有实体、Mapper、Service、Controller 接入。
- 当前问答不写入 conversation 表。

### 4.10 message

职责：

- 预留会话消息表。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 消息主键 |
| `conversation_id` | 所属会话 |
| `role` | USER 或 ASSISTANT |
| `content` | 消息内容 |
| `message_type` | QUESTION 或 ANSWER |
| `quote_json` | 引用来源 JSON |
| `created_at` | 创建时间 |

当前注意点：

- 当前没有实体、Mapper、Service、Controller 接入。
- 当前问答不写入 message 表。

## 5. document 状态流转

### 5.1 当前已使用状态

当前代码中实际出现的 document parseStatus 包括：

| 状态 | 含义 | 当前来源 |
| --- | --- | --- |
| `UPLOADED` | 文档元数据已保存，尚未解析/切片 | 文档上传 |
| `PENDING` | 已创建解析任务，等待处理 | 创建 parse task |
| `CHUNKING` | 正在执行切片 | process 开始 |
| `CHUNKED` | 切片完成 | process 成功 |

实体注释中提到 `DONE`、`FAILED`，但当前代码主要使用 `CHUNKED` 表达切片完成，失败状态尚未形成完整落库闭环。

### 5.2 当前状态流转

```text
上传元数据
  -> UPLOADED

创建解析任务
  UPLOADED -> PENDING

执行 process
  UPLOADED/PENDING -> CHUNKING -> CHUNKED
```

### 5.3 第二周建议状态流转

```text
UPLOADED
  -> PENDING
  -> CHUNKING
  -> CHUNKED

UPLOADED/PENDING/CHUNKING
  -> FAILED
```

建议说明：

- `FAILED` 应表示文档解析或切片失败。
- 失败原因优先记录到 `task_record.error_message`。
- 如果后续需要文档级失败原因，可新增 document.error_message，但第二周可先不加。

### 5.4 状态边界

- document.parseStatus 只表达“文档解析/切片”状态。
- embedding 状态不应混入 document.parseStatus。
- 问答命中状态不应混入 document.parseStatus。

## 6. task_record 状态流转

### 6.1 当前已使用状态

| 状态 | 含义 | 当前完成度 |
| --- | --- | --- |
| `PENDING` | 任务已创建，等待执行 | 已使用 |
| `RUNNING` | 任务执行中 | 实体注释提到，当前闭环不足 |
| `SUCCESS` | 任务成功 | 实体注释提到，当前闭环不足 |
| `FAILED` | 任务失败 | 实体注释提到，当前闭环不足 |

### 6.2 当前状态流转

```text
无任务 -> PENDING
```

当前问题：

- 已有任务创建，但缺少执行器。
- 缺少 `RUNNING/SUCCESS/FAILED` 完整更新。
- 缺少任务查询接口。

### 6.3 第二周建议状态流转

```text
PENDING -> RUNNING -> SUCCESS
PENDING -> RUNNING -> FAILED
FAILED -> PENDING/RUNNING -> SUCCESS
```

说明：

- 第二周可以同步执行任务，不强制引入异步队列。
- 失败重试时可以复用原任务或创建新任务，但必须有明确策略。
- 对同一 document 的 pending/running parse 任务应避免重复创建。

### 6.4 task_type 建议

当前已使用：

- `DOCUMENT_PARSE`

第二周可扩展：

- `DOCUMENT_EMBEDDING`
- `DOCUMENT_INDEX`

是否扩展取决于第二周实现范围。如果 embed 仍保持同步执行且已有 chunk_embedding 状态足够，可以暂不新增 embedding 任务。

## 7. chunk_embedding 状态流转

### 7.1 当前已使用状态

| 状态 | 含义 | 当前完成度 |
| --- | --- | --- |
| `PENDING` | 已创建向量化状态，尚未处理 | 已使用 |
| `PROCESSING` | 正在向量化 | 已使用 |
| `SUCCESS` | 向量化成功 | 已使用 |
| `FAILED` | 向量化失败 | 已使用 |

### 7.2 当前状态流转

```text
无记录 -> PENDING -> PROCESSING -> SUCCESS
无记录 -> PENDING -> PROCESSING -> FAILED

已有记录 -> PENDING -> PROCESSING -> SUCCESS/FAILED
```

说明：

- `chunk_id` 唯一，一个 chunk 只保留一条向量化状态。
- 重复执行 embedding 时，会重置状态、vectorId、error、embeddedAt。

### 7.3 第二周真实向量接入后的语义

```text
PENDING
  -> PROCESSING
  -> SUCCESS
      vector_id = 真实向量库 ID
      embedded_at = 当前时间

PENDING
  -> PROCESSING
  -> FAILED
      embedding_error = 失败原因
      vector_id = null
```

### 7.4 状态边界

- `chunk_embedding.status` 只表达“chunk 是否完成 embedding/索引同步”。
- 不表达文档是否解析完成。
- 不表达检索质量。
- 不表达 LLM 问答状态。

## 8. chat_record 与 chat_feedback 的关系

### 8.1 chat_record 的当前职责

`chat_record` 是当前问答链路的核心事实表，一次调用 `/api/chat/ask` 成功后，应保存一条记录。

它承载：

- 用户是谁。
- 使用哪个知识库。
- 属于哪个 conversationId。
- 问了什么。
- 回答了什么。
- 是否命中知识库内容。
- 命中了多少 chunk。
- 本次 topK。
- 引用了哪些来源。

### 8.2 chat_feedback 的当前职责

`chat_feedback` 是问答质量反馈表，记录用户对某条 `chat_record` 的评价。

当前支持：

- `LIKE`
- `DISLIKE`
- 可选 comment

### 8.3 关系和约束

```text
chat_record.id 1 -> N chat_feedback.chat_record_id
chat_feedback(chat_record_id, user_id) 唯一
```

当前业务约束：

- 用户只能反馈自己的问答记录。
- 同一用户不能重复反馈同一条问答记录。

### 8.4 未命中问题模型

当前没有单独的 missed_question 表。

未命中问题来源：

```text
chat_record.matched = false
```

管理端未命中问题查询直接复用 chat_record。

优点：

- 模型简单。
- 不需要同步 missed_question 数据。
- 问答记录和未命中问题天然一致。

局限：

- 无法标记未命中问题是否已处理。
- 无法记录补充知识后的处理人、处理时间、处理状态。
- 后续知识运营需要升级。

## 9. 当前 conversationId 设计的定位与局限

### 9.1 当前定位

当前 conversationId 是“轻量多轮上下文标识”，不是完整会话模型。

实现方式：

- 提问时不传 conversationId，服务端生成 UUID。
- 提问时传 conversationId，服务端查询同一用户、同一知识库、同一 conversationId 下最近 5 条 chat_record。
- 最近历史问答传给答案生成服务作为上下文。

### 9.2 当前优点

- 改动小。
- 不依赖 conversation/message 表。
- 已能支持基础追问上下文。
- chat_record 一张表即可回溯最近问答。

### 9.3 当前局限

- 没有会话标题。
- 没有会话列表接口。
- 没有 message 粒度的 USER/ASSISTANT 消息表。
- 没有会话归档、删除、重命名。
- 无法很好支持前端完整会话窗口。
- conversationId 是字符串，不关联 conversation 表主键。
- 不能记录单条 message 的引用来源，只能记录一次问答的 citations。

### 9.4 第二周建议

第二周不强制升级完整 conversation/message 模型。

原因：

- 当前核心目标是打穿真实文件处理和真实 RAG 链路。
- conversation/message 会扩大实现范围。
- 当前 conversationId 已足够支撑基础多轮验收。

建议后续升级：

```text
conversation
  -> message
  -> chat_record 或 qa_trace
```

升级时需要明确：

- chat_record 是否继续保留为问答事实表。
- message 是否只负责展示。
- citations 应存在 chat_record 还是 message.quote_json。
- conversationId 字符串如何迁移到 conversation.id。

## 10. 已经足够支撑第二周的数据模型

以下模型足够支撑第二周目标，可以继续使用：

### 10.1 user

足够支撑：

- 注册。
- 登录。
- JWT 当前用户识别。
- ADMIN 管理端权限判断。

第二周建议小补强：

- 登录时校验 status。

### 10.2 knowledge_base

足够支撑：

- 用户知识库归属。
- 文档权限隔离。
- 检索和问答的知识库边界。

第二周无需大改。

### 10.3 document

足够支撑：

- 文档元数据。
- 真实 storagePath。
- 解析状态。
- 上传人和知识库关系。

第二周可继续使用。

注意：

- 失败原因可先放 task_record，不必急着加 document.error_message。

### 10.4 task_record

基本足够支撑：

- 文档解析任务。
- 任务状态。
- 失败原因。
- 重试次数。

第二周需要补的是业务逻辑和查询接口，不一定需要改表。

### 10.5 document_chunk

足够支撑：

- 文档切片。
- 检索候选内容。
- citations 内容来源。

第二周无需大改。

### 10.6 chunk_embedding

基本足够支撑：

- 每个 chunk 的向量化状态。
- 模型名。
- vectorId。
- 失败原因。
- 文档级 embedding 状态汇总。

第二周接真实向量库时可继续使用。

注意：

- 如果真实向量库需要额外 indexName、namespace、dimension，可先通过配置管理，不一定立刻加字段。

### 10.7 chat_record

足够支撑：

- 问答记录。
- 轻量多轮 conversationId。
- 未命中问题查询。
- citations 追溯。
- 用户历史问答。
- 管理端问答日志。

第二周建议补强：

- 查询详情时结构化返回 citations。
- 如需区分 mock/real，可考虑响应中返回模式，暂不强制加字段。

### 10.8 chat_feedback

足够支撑：

- 用户点赞/点踩。
- 防重复反馈。
- 后续质量分析基础数据。

第二周无需大改。

## 11. 临时设计与后续升级方向

### 11.1 document.storage_path

当前定位：

- 第一周可以是占位路径。
- 第二周应升级为真实文件路径。

后续升级：

- 抽象文件存储 provider。
- 支持本地存储和对象存储。
- 存储原始文件 hash、MIME 类型、存储桶、对象 key。

### 11.2 document.parse_status

当前定位：

- 表达文档解析/切片状态。

临时问题：

- 状态常量分散。
- 失败状态闭环不足。

后续升级：

- 使用统一枚举。
- 明确 `FAILED`。
- 如需要，新增 `parse_error` 或通过任务表统一承载。

### 11.3 task_record

当前定位：

- 通用任务记录表。

临时问题：

- 没有 userId。
- 没有任务进度百分比。
- 没有开始/结束时间。
- 没有任务锁或并发控制字段。

后续升级：

- 增加 started_at、finished_at。
- 增加 progress。
- 增加 operator_id 或 created_by。
- 增加锁定字段或调度标识。
- 支持异步任务队列。

### 11.4 chunk_embedding

当前定位：

- 记录 chunk 的向量化状态和 vectorId。

临时问题：

- 不保存向量维度。
- 不保存向量库类型、namespace、collection。
- 不保存索引版本。

后续升级：

- 增加 vector_store、namespace、index_name。
- 增加 embedding_dimension。
- 增加 index_version。
- 支持同一 chunk 多模型、多版本 embedding。

### 11.5 chat_record.citations_json

当前定位：

- 保存引用来源 JSON。

临时问题：

- JSON 字段不利于复杂查询。
- 字段结构变更需要兼容历史数据。
- 无法直接按 citation 统计引用频率。

后续升级：

- 可引入 `chat_citation` 表。
- 每条 citation 单独落库。
- 支持引用统计和来源质量分析。

### 11.6 chat_record.conversation_id

当前定位：

- 轻量多轮会话标识。

临时问题：

- 不是 conversation 表外键。
- 无会话标题和列表。
- 无 message 粒度记录。

后续升级：

- 接入 conversation/message 表。
- chat_record 作为问答事实表保留。
- conversationId 逐步迁移为 conversation.id 或稳定外部 ID。

### 11.7 missed question

当前定位：

- 使用 `chat_record.matched=false` 表达未命中问题。

临时问题：

- 无处理状态。
- 无处理人。
- 无补充知识库后的关联关系。

后续升级：

- 新增 `missed_question` 表，或在 chat_record 上增加运营处理字段。
- 支持 `PENDING`、`RESOLVED`、`IGNORED`。
- 关联补充的 document 或 chunk。

### 11.8 user / role

当前定位：

- 简单 USER/ADMIN 区分。

临时问题：

- 无用户管理接口。
- 无组织、部门、细粒度权限。

后续升级：

- 增加用户管理。
- 增加角色表和权限表。
- 增加知识库成员权限。

## 12. 第二周数据模型使用建议

第二周开发时建议遵循：

- 不大规模重构表结构。
- 优先补业务闭环和状态流转。
- 真实文件上传优先复用 document.storage_path。
- 解析失败优先写 task_record.error_message。
- embedding 失败优先写 chunk_embedding.embedding_error。
- 问答引用继续写 chat_record.citations_json。
- 多轮继续使用 chat_record.conversation_id。
- 所有查询必须通过 userId 和 knowledgeBaseId 做数据隔离。

如果第二周确实需要新增字段，应优先满足以下原则：

- 能支撑验收。
- 不破坏已有数据。
- 通过 Flyway 迁移。
- 字段含义稳定，不为临时调试随意加字段。

## 13. 当前主链路数据流

### 13.1 文档处理数据流

```text
user
  -> knowledge_base
    -> document
      -> task_record(DOCUMENT_PARSE)
      -> document_chunk
```

步骤说明：

1. 用户创建知识库。
2. 用户上传文档，生成 document。
3. 用户创建或执行解析任务，生成 task_record。
4. 文档解析和切片后写入 document_chunk。
5. document.parse_status 更新为 CHUNKED。

### 13.2 向量化数据流

```text
document
  -> document_chunk
    -> chunk_embedding
      -> external vector store
```

步骤说明：

1. 读取文档下所有 chunk。
2. 为每个 chunk 创建或重置 chunk_embedding。
3. 调用 embedding 服务。
4. 成功后保存 vectorId、status、embeddedAt。
5. 失败后保存 error。

### 13.3 问答数据流

```text
user question
  -> retrieval_search
    -> document_chunk + chunk_embedding
      -> citations
        -> answer_generator
          -> chat_record
```

步骤说明：

1. 用户在知识库下提问。
2. 系统校验知识库归属。
3. 系统检索相关 chunk。
4. 命中则生成 citations。
5. 答案生成器生成答案。
6. 保存 chat_record。
7. 返回 answer、matched、citations、conversationId。

### 13.4 反馈与未命中数据流

```text
chat_record
  -> chat_feedback

chat_record.matched=false
  -> admin missed question list
```

步骤说明：

1. 用户对自己的 chat_record 提交反馈。
2. 系统写入 chat_feedback。
3. 管理端通过 chat_record.matched=false 查询未命中问题。

## 14. 结论

当前数据模型已经足够支撑第二周的最小目标：真实文件上传、真实解析切片、任务状态闭环、embedding 状态、检索、问答记录、反馈和未命中查询。

第二周不建议优先重构数据模型。更合理的策略是：

- 先复用现有表补齐真实链路。
- 只在必要时通过 Flyway 小步新增字段。
- 把 conversation/message、missed_question、chat_citation、复杂权限模型放到后续版本。

当前最需要补强的不是表数量，而是状态流转、真实文件路径、任务执行、mock/real 模式区分和权限隔离的一致性。
