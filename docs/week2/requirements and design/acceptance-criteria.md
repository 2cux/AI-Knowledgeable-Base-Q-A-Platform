# 第二周验收标准

## 1. 文档定位

本文档用于定义第二周关键能力的验收标准。验收重点不是“接口是否返回 200”，而是确认真实 MVP 主链路是否可用：

```text
真实文件上传
  -> 真实文件存储
  -> 真实文件解析
  -> 文档切片
  -> parse/embed 任务状态闭环
  -> embedding
  -> 检索
  -> LLM 问答
  -> 问答记录、反馈、管理查询
```

验收结论分为两类：

- 必须通过：第二周 MVP 能否成立的硬性标准。
- 可接受但需后续优化：不阻塞第二周验收，但必须记录为后续任务。

## 2. 验收前置条件

第二周验收开始前，应准备以下条件：

- 后端应用可以启动，项目可以编译通过。
- MySQL 可用，Flyway 迁移已执行成功。
- 至少存在一个普通用户账号。
- 至少存在一个 ADMIN 用户账号。
- 普通用户可以登录并获取 JWT。
- ADMIN 用户可以登录并访问管理端接口。
- 准备至少两个知识库，分别归属于不同用户，用于权限隔离验收。
- 准备测试文件：
  - 一个有效 txt 文件。
  - 一个有效 md 文件。
  - 一个空文件。
  - 一个超出大小限制的文件。
  - 一个不支持后缀的文件。
- 如果接入真实 AI 能力，应准备：
  - embedding 服务配置。
  - 向量检索服务配置。
  - LLM 服务配置。
- 如果真实 AI 外部服务暂不可用，应明确使用 mock 模式，并在验收结论中标注“AI 真实能力未通过，仅完成真实文件处理闭环”。

## 3. 真实文件上传 / 存储验收

### 3.1 验收对象

- `POST /api/documents/upload-file`
- 文件存储配置。
- 文档元数据入库逻辑。
- 原 `POST /api/documents/upload` 兼容性。

### 3.2 验收前提

- 用户已登录并携带有效 JWT。
- 用户已创建一个知识库。
- 后端已配置受控文件存储目录。

### 3.3 核心验收点

- 文件必须真实保存到后端受控目录。
- document 表必须写入真实文件元数据。
- `storagePath` 必须由服务端生成或严格校验，不能直接信任客户端路径。
- 上传成功不代表解析成功，文档初始状态应清晰，例如 `UPLOADED`。
- 原元数据上传接口继续可用，但应明确不是文件上传主入口。

### 3.4 正常流程验收

步骤：

1. 调用 `POST /api/documents/upload-file` 上传 txt 文件。
2. 请求参数包含 `knowledgeBaseId` 和 `file`。
3. 接口返回文档详情。
4. 调用 `GET /api/documents/{id}` 查询文档详情。

预期结果：

- 返回 `id`、`knowledgeBaseId`、`fileName`、`fileType`、`fileSize`、`storagePath`、`parseStatus`。
- `parseStatus` 为 `UPLOADED` 或等价初始状态。
- 服务器文件目录下可以找到对应文件。
- `fileSize` 与上传文件大小一致。

### 3.5 异常流程验收

必须验证：

- 未传文件时返回参数错误。
- `knowledgeBaseId` 不存在时返回业务错误。
- 文件为空时返回业务错误，或允许上传但后续解析明确失败；推荐直接拒绝。
- 文件超过大小限制时返回业务错误。
- 不支持的文件后缀返回业务错误。
- 文件名包含 `../`、`\` 等路径穿越内容时不能影响实际保存路径。
- 文件保存失败时不得创建可用 document 记录。
- 数据库写入失败时不能留下不可追踪的孤立文件；如果暂未实现清理，必须记录为后续优化。

### 3.6 权限验收

必须验证：

- 未登录用户不能上传文件。
- 用户 A 不能向用户 B 的知识库上传文件。
- 用户 A 只能查询自己上传或自己知识库下的文档。

### 3.7 数据落库验收

必须检查：

- `document.knowledge_base_id` 正确。
- `document.file_name` 正确。
- `document.file_type` 正确。
- `document.file_size` 正确。
- `document.storage_path` 为真实存储路径或服务端可解析路径。
- `document.created_by` 为当前用户 ID。
- `document.parse_status` 为初始状态。

### 3.8 状态流转验收

上传阶段状态：

```text
无记录 -> UPLOADED
```

必须确认：

- 上传成功后不能直接变成 `CHUNKED` 或 `SUCCESS`。
- 上传失败不能产生正常 document 状态。

### 3.9 验收结论划分

必须通过：

- 真实文件保存成功。
- document 元数据入库成功。
- 用户和知识库权限隔离正确。
- 文件类型、大小、路径安全校验有效。

可接受但需后续优化：

- 暂不支持 pdf/doc/docx 真实解析。
- 上传失败后的物理文件自动清理暂未完善，但必须记录。
- 文件重复上传去重暂未实现。

## 4. 真实文件解析与切片验收

### 4.1 验收对象

- txt/md 文件解析服务。
- `POST /api/documents/{id}/process`
- `POST /api/documents/{id}/parse/run`
- `GET /api/documents/{id}/chunks`

### 4.2 验收前提

- 已上传一个有效 txt 或 md 文件。
- 文档状态为 `UPLOADED` 或 `PENDING`。
- 当前用户拥有该文档所属知识库。

### 4.3 核心验收点

- 不传 `textContent` 时，系统能从 `storagePath` 读取真实文件。
- 解析出的文本能进入现有切片逻辑。
- 切片结果写入 `document_chunk`。
- 重复处理时旧 chunk 不应重复堆积。
- 解析失败必须有明确错误和状态。

### 4.4 正常流程验收

步骤：

1. 上传 txt/md 文件。
2. 调用 `POST /api/documents/{id}/parse/run` 或 `POST /api/documents/{id}/process`，不传 `textContent`。
3. 查询文档详情。
4. 调用 `GET /api/documents/{id}/chunks`。

预期结果：

- 文档处理成功。
- 文档状态最终为 `CHUNKED` 或等价成功状态。
- `document_chunk` 中生成至少 1 条记录。
- chunk 的 `document_id`、`knowledge_base_id` 正确。
- chunk 的 `chunk_index` 从 0 开始且顺序稳定。
- chunk 内容来自真实文件。

### 4.5 异常流程验收

必须验证：

- 文件不存在时解析失败，错误信息明确。
- 空文件解析失败或不生成 chunk，错误信息明确。
- 不支持的文件类型解析失败，错误信息明确。
- `overlap >= chunkSize` 时返回参数错误。
- 文档正在处理时再次触发处理，应返回明确错误或复用任务，不能并发生成重复 chunk。
- 处理中发生异常时，文档不能永久停留在 `CHUNKING`。

### 4.6 权限验收

必须验证：

- 用户 A 不能解析用户 B 的文档。
- 用户 A 不能查询用户 B 文档的 chunks。
- 未登录用户不能调用解析或切片接口。

### 4.7 数据落库验收

必须检查：

- `document_chunk.document_id` 正确。
- `document_chunk.knowledge_base_id` 正确。
- `document_chunk.chunk_index` 连续。
- `document_chunk.content` 非空。
- `document_chunk.token_count` 有值或符合当前实现口径。
- 重新处理后同一文档的旧 chunk 不重复残留。

### 4.8 状态流转验收

正常状态：

```text
UPLOADED/PENDING -> CHUNKING -> CHUNKED
```

失败状态：

```text
UPLOADED/PENDING -> CHUNKING -> FAILED
```

必须确认：

- 成功时最终状态可查询。
- 失败时有错误信息。
- 失败后允许重新处理，或明确不允许并给出错误。

### 4.9 验收结论划分

必须通过：

- txt/md 可从真实文件解析并切片。
- 切片入库正确。
- 失败状态可追踪。
- 权限隔离正确。

可接受但需后续优化：

- 暂不支持 PDF 或仅支持文本型 PDF。
- 暂不支持 OCR。
- 暂未做复杂分段策略，仅按字符长度切片。

## 5. parse/embed 任务闭环验收

### 5.1 验收对象

- `task_record`
- `POST /api/documents/{id}/parse`
- `POST /api/documents/{id}/parse/run`
- `GET /api/tasks/{id}`
- `GET /api/documents/{id}/tasks`
- `POST /api/documents/{id}/embed`
- `GET /api/documents/{id}/embedding-status`

### 5.2 验收前提

- 已有文档记录。
- 已上传真实文件。
- parse 任务和 embed 任务对应服务已实现或保留当前同步执行能力。

### 5.3 核心验收点

- parse 任务不能只创建记录，必须能执行或有明确执行入口。
- 任务状态必须可查询。
- 任务失败必须可追踪。
- embed 任务或执行过程必须能反映每个 chunk 的处理状态。

### 5.4 正常流程验收

parse 流程：

1. 调用 `POST /api/documents/{id}/parse` 创建任务。
2. 返回 `taskId` 和任务状态。
3. 调用 `POST /api/documents/{id}/parse/run` 执行任务，或确认创建后自动执行。
4. 调用 `GET /api/tasks/{taskId}` 查询任务状态。

预期结果：

- 任务状态从 `PENDING` 或 `RUNNING` 最终变为 `SUCCESS`。
- 文档状态最终为 `CHUNKED`。
- chunk 已生成。

embed 流程：

1. 文档已切片。
2. 调用 `POST /api/documents/{id}/embed`。
3. 调用 `GET /api/documents/{id}/embedding-status`。

预期结果：

- 每个 chunk 有 embedding 状态。
- 成功时 `successCount == totalChunks`。
- 文档向量化汇总状态为 `SUCCESS` 或等价成功状态。

### 5.5 异常流程验收

必须验证：

- 未切片文档执行 embed 时返回明确错误。
- parse 过程中解析失败时任务为 `FAILED`。
- embed 过程中单个 chunk 失败时记录失败原因。
- 重复创建同一文档 pending parse 任务时不能无限创建重复任务。
- 查询不存在的 taskId 返回 404 或业务错误。

### 5.6 权限验收

必须验证：

- 用户只能查询自己可访问文档关联的任务。
- 用户 A 不能查询用户 B 文档的任务。
- 用户 A 不能对用户 B 文档执行 parse 或 embed。

### 5.7 数据落库验收

必须检查：

- `task_record.task_type` 正确，例如 `DOCUMENT_PARSE`。
- `task_record.biz_type` 正确，例如 `DOCUMENT`。
- `task_record.biz_id` 为文档 ID。
- `task_record.status` 正确流转。
- `task_record.error_message` 在失败时有值。
- `chunk_embedding` 中每个 chunk 至少有一条状态记录。

### 5.8 状态流转验收

parse 任务：

```text
PENDING -> RUNNING -> SUCCESS
PENDING -> RUNNING -> FAILED
```

embed chunk：

```text
PENDING -> PROCESSING -> SUCCESS
PENDING -> PROCESSING -> FAILED
```

必须确认：

- 任务不会永久停留在 `RUNNING`。
- 失败可以查询到原因。
- 成功和失败计数准确。

### 5.9 验收结论划分

必须通过：

- parse 任务可执行、可查询、有状态。
- embed 状态可查询。
- 失败可追踪。
- 权限隔离正确。

可接受但需后续优化：

- parse/run 第二周可以同步执行，不强制异步。
- embed 第二周可以同步执行，不强制任务队列。
- 不要求分布式任务调度。

## 6. 真实 embedding 与检索验收

### 6.1 验收对象

- 真实 `EmbeddingClient`。
- 真实 `VectorSearchAdapter`。
- `POST /api/documents/{id}/embed`
- `GET /api/documents/{id}/embedding-status`
- `POST /api/retrieval/search`

### 6.2 验收前提

- 文档已切片。
- 已配置真实 embedding 服务和向量检索服务。
- 如果真实服务不可用，应启用 mock 模式并明确记录。

### 6.3 核心验收点

- chunk 能通过真实 embedding 服务生成向量。
- 向量能写入向量索引或可检索存储。
- 检索时能按 knowledgeBaseId 过滤。
- 查询问题能召回相关 chunk。
- 响应中能区分 mock 和 real 模式。

### 6.4 正常流程验收

步骤：

1. 对已切片文档调用 `POST /api/documents/{id}/embed`。
2. 查询 `GET /api/documents/{id}/embedding-status`。
3. 调用 `POST /api/retrieval/search`，传入与文档内容相关的问题。

预期结果：

- `embedding-status.totalChunks > 0`。
- `successCount > 0`。
- 成功 chunk 有 `embeddingModel` 和 `vectorId`。
- 检索返回 `total > 0`。
- 返回 chunk 内容与问题相关。
- 返回 `score`。
- 如果是真实检索，返回或日志中能确认 `retrievalMode=VECTOR`。

### 6.5 异常流程验收

必须验证：

- embedding 服务不可用时，chunk 状态变为 `FAILED` 或接口返回明确错误。
- 空 chunk 不应调用真实 embedding，或调用失败后记录明确原因。
- 向量索引写入失败时不能标记为完全成功。
- 检索 topK 超出限制时返回参数错误。
- 知识库没有任何成功 embedding 时，检索返回空结果，不应报系统异常。

### 6.6 权限验收

必须验证：

- 用户只能对自己知识库下的文档执行 embedding。
- 用户只能在自己的知识库中检索。
- 用户 A 的问题不能召回用户 B 知识库的 chunk。

### 6.7 数据落库验收

必须检查：

- `chunk_embedding.document_id` 正确。
- `chunk_embedding.knowledge_base_id` 正确。
- `chunk_embedding.chunk_id` 正确。
- `chunk_embedding.embedding_model` 正确。
- 成功时 `vector_id` 非空。
- 成功时 `status=SUCCESS`。
- 失败时 `status=FAILED` 且 `embedding_error` 有值。

### 6.8 状态流转验收

```text
未向量化 -> PENDING -> PROCESSING -> SUCCESS
未向量化 -> PENDING -> PROCESSING -> FAILED
```

必须确认：

- 重复 embed 时状态可以重新进入处理流程。
- 旧失败记录可以被后续成功覆盖，或有明确重试策略。

### 6.9 验收结论划分

必须通过：

- 在 mock 模式下，现有 embedding 和 retrieval 链路不能退化。
- 在 real 模式配置齐全时，真实 embedding 能完成至少一个 chunk。
- 检索必须按知识库隔离。

可接受但需后续优化：

- 真实向量库暂未接入时，可以保留 mock 检索，但必须在验收报告中标注。
- score 归一化暂不统一。
- 向量索引删除和重建策略暂不完善。

## 7. 真实 LLM 问答验收

### 7.1 验收对象

- 真实 `AnswerGeneratorService`。
- `POST /api/chat/ask`
- `GET /api/chat/records/{id}`
- `GET /api/admin/chat/records/{id}`

### 7.2 验收前提

- 用户已登录。
- 知识库下已有已切片、已向量化并可检索的文档。
- LLM 服务配置可用。

### 7.3 核心验收点

- 问答必须先检索，再生成答案。
- 答案应基于检索上下文。
- 返回 citations。
- 未命中时不编造答案。
- 问答记录必须落库。
- conversationId 继续可用。

### 7.4 正常流程验收

步骤：

1. 调用 `POST /api/chat/ask`，传入知识库 ID 和与文档内容相关的问题。
2. 查看响应。
3. 调用 `GET /api/chat/records/{id}` 或查询问答记录列表后取详情。

预期结果：

- 返回 `answer`。
- `matched=true`。
- `retrievedChunkCount > 0`。
- `citations` 非空。
- `citations[].chunkId` 能对应到真实 chunk。
- 返回 `conversationId`。
- 如果为真实 LLM，返回或日志中能确认 `answerMode=LLM` 和模型名。

### 7.5 异常流程验收

必须验证：

- 知识库无相关内容时，`matched=false`，答案为知识不足或未命中提示。
- LLM 服务不可用时，返回明确错误或降级答案，并记录失败原因。
- 问题为空或超长时返回参数错误。
- 传入不存在的 conversationId 时，按当前设计返回会话不存在。
- citations 序列化失败时不能保存半条异常记录；若发生应返回明确错误。

### 7.6 权限验收

必须验证：

- 用户只能在自己的知识库下提问。
- 用户 A 不能使用用户 B 的 knowledgeBaseId 发起问答。
- 用户 A 不能使用用户 B 的 conversationId 获取历史上下文。

### 7.7 数据落库验收

必须检查 `chat_record`：

- `user_id` 为当前用户。
- `knowledge_base_id` 正确。
- `conversation_id` 正确。
- `question` 正确。
- `answer` 非空。
- `matched` 与检索结果一致。
- `retrieved_chunk_count` 与 citations 或检索结果一致。
- `top_k` 正确。
- `citations_json` 可解析。

### 7.8 状态流转验收

问答本身当前不是任务化流程，但必须验证逻辑状态：

```text
提问 -> 检索 -> 生成答案 -> 保存问答记录 -> 返回响应
```

必须确认：

- 检索失败时不应保存伪成功问答记录。
- 答案生成失败时不应标记为正常成功问答，除非有明确降级策略。
- matched 的值必须反映检索是否命中，而不是 LLM 是否返回文本。

### 7.9 验收结论划分

必须通过：

- 问答接口可用。
- 检索命中时返回 citations。
- 未命中时不编造答案。
- 问答记录落库。
- 权限隔离正确。

可接受但需后续优化：

- LLM 暂未接入时可保留占位答案，但必须标注 `answerMode=PLACEHOLDER` 或在验收结论中说明。
- 暂不要求流式输出。
- 暂不要求复杂 prompt 管理。

## 8. 问答记录、反馈、管理查询回归验收

### 8.1 验收对象

- `GET /api/chat/records`
- `GET /api/chat/records/{id}`
- `POST /api/chat/records/{id}/feedback`
- `GET /api/admin/chat/records`
- `GET /api/admin/chat/records/{id}`
- `GET /api/admin/chat/missed`

### 8.2 验收前提

- 已产生至少一条命中问答记录。
- 已产生至少一条未命中问答记录。
- 普通用户和 ADMIN 用户均可登录。

### 8.3 核心验收点

- 用户能查自己的问答记录。
- 用户不能查别人的问答记录。
- 用户能提交 LIKE/DISLIKE 反馈。
- 同一用户不能重复反馈同一记录。
- 管理员能查全量问答日志。
- 管理员能查未命中问题。
- 问答详情应返回 citations 和 conversationId。

### 8.4 正常流程验收

用户端：

1. 调用 `GET /api/chat/records`。
2. 调用 `GET /api/chat/records/{id}`。
3. 调用 `POST /api/chat/records/{id}/feedback` 提交 LIKE。

预期结果：

- 列表返回当前用户记录。
- 详情返回完整问题、答案、matched、retrievedChunkCount、conversationId、citations。
- 反馈提交成功。

管理端：

1. ADMIN 调用 `GET /api/admin/chat/records`。
2. ADMIN 调用 `GET /api/admin/chat/records/{id}`。
3. ADMIN 调用 `GET /api/admin/chat/missed`。

预期结果：

- 管理员可查看系统问答记录。
- 管理详情包含排障字段。
- 未命中列表只包含 `matched=false` 的记录。

### 8.5 异常流程验收

必须验证：

- 用户查询不存在的记录返回 404 或业务错误。
- 用户查询他人记录返回 404 或无权限。
- 重复反馈同一记录返回冲突错误。
- feedbackType 非 LIKE/DISLIKE 返回参数错误。
- 非 ADMIN 用户访问管理端接口返回 403。

### 8.6 权限验收

必须验证：

- 普通用户不能访问 `/api/admin/**`。
- ADMIN 用户可以访问管理端日志。
- 用户记录查询严格按 userId 隔离。
- 管理端查询不受普通用户隔离限制，但必须要求 ADMIN。

### 8.7 数据落库验收

必须检查：

- `chat_feedback.chat_record_id` 正确。
- `chat_feedback.user_id` 正确。
- `chat_feedback.feedback_type` 正确。
- `chat_feedback.comment` 按规则保存。
- 同一 `chat_record_id + user_id` 不重复。

### 8.8 状态流转验收

反馈状态不是显式状态机，但必须验证：

```text
未反馈 -> 已反馈
已反馈 -> 再次反馈失败
```

管理未命中问题依赖：

```text
问答检索无结果 -> chat_record.matched=false -> 管理端 missed 可查询
```

### 8.9 验收结论划分

必须通过：

- 问答记录查询不回归。
- 反馈可提交且防重复。
- 管理日志和未命中问题可查。
- 权限隔离正确。

可接受但需后续优化：

- 反馈暂不支持修改或撤销。
- 未命中问题暂不支持处理状态标记。
- 管理端暂不支持复杂筛选和统计。

## 9. 权限与数据隔离总体验收

### 9.1 验收对象

- 所有 `/api/**` 业务接口。
- 所有 `/api/admin/**` 管理接口。

### 9.2 验收前提

- 至少两个普通用户。
- 每个用户各有一个知识库、文档和问答记录。
- 至少一个 ADMIN 用户。

### 9.3 核心验收点

- 未登录不能访问业务接口。
- 普通用户只能访问自己的知识库、文档、任务、问答记录。
- 普通用户不能访问管理端。
- ADMIN 可以访问管理端。

### 9.4 正常流程验收

- 用户 A 使用自己的 token 操作自己的知识库和文档成功。
- 用户 B 使用自己的 token 操作自己的知识库和文档成功。
- ADMIN 使用自己的 token 查询管理端日志成功。

### 9.5 异常流程验收

- 无 token 访问业务接口返回 401。
- 无效 token 返回 401。
- 用户 A 使用用户 B 的 knowledgeBaseId 上传、解析、检索、问答失败。
- 用户 A 查询用户 B 的 taskId 失败。
- 普通用户访问 `/api/admin/chat/records` 失败。

### 9.6 权限验收

必须通过全部权限场景。

### 9.7 数据落库验收

必须确认：

- 用户 A 的操作不会写入用户 B 的知识库。
- 任务、文档、chunk、embedding、chat_record 的业务归属一致。

### 9.8 状态流转验收

权限失败不能产生状态变化：

- 越权上传不能创建 document。
- 越权 parse 不能创建 task。
- 越权 embed 不能创建 chunk_embedding。
- 越权 ask 不能创建 chat_record。

### 9.9 验收结论划分

必须通过：

- 所有跨用户越权场景均被拦截。
- 管理端 ADMIN 校验有效。

可接受但需后续优化：

- 细粒度 RBAC 暂不实现。
- 用户管理后台暂不完整。

## 10. 数据一致性与回归验收

### 10.1 验收对象

- `document`
- `document_chunk`
- `task_record`
- `chunk_embedding`
- `chat_record`
- `chat_feedback`

### 10.2 验收前提

- 已完成一次从上传到问答的完整流程。

### 10.3 核心验收点

- 主链路每一步都有可追踪数据。
- 失败时不会产生伪成功数据。
- 重复执行不会产生明显重复脏数据。

### 10.4 正常流程验收

完整流程后应存在：

- 1 条 document。
- 多条 document_chunk。
- 至少 1 条 task_record。
- 多条 chunk_embedding 或等价 embedding 状态记录。
- 1 条 chat_record。
- 可选 1 条 chat_feedback。

### 10.5 异常流程验收

必须验证：

- 文件上传失败不产生 document。
- 解析失败不产生成功任务。
- embed 失败不产生成功状态。
- 问答失败不产生伪成功记录。

### 10.6 权限验收

数据查询必须遵守用户归属隔离，管理端除外。

### 10.7 数据落库验收

必须确认外键或业务字段能串联：

```text
knowledge_base.id
  -> document.knowledge_base_id
  -> document_chunk.knowledge_base_id
  -> chunk_embedding.knowledge_base_id
  -> chat_record.knowledge_base_id
```

### 10.8 状态流转验收

必须能从数据库看出主链路状态：

```text
document.parse_status
task_record.status
chunk_embedding.status
chat_record.matched
```

### 10.9 验收结论划分

必须通过：

- 数据能串联。
- 关键状态能追踪。
- 失败不伪装为成功。

可接受但需后续优化：

- 暂未做完整物理删除和级联清理。
- 暂未做复杂审计日志。

## 11. 第二周总体验收流程

建议按以下顺序执行总体验收：

1. 注册普通用户 A。
2. 登录用户 A，获取 token。
3. 用户 A 创建知识库。
4. 用户 A 上传 txt/md 文件。
5. 查询文档详情，确认文件元数据。
6. 执行 parse/run。
7. 查询任务详情。
8. 查询文档 chunks。
9. 执行 embed。
10. 查询 embedding-status。
11. 调用 retrieval/search。
12. 调用 chat/ask，问题应与文档内容相关。
13. 查询 chat records。
14. 查询 chat record detail，确认 citations。
15. 提交 feedback。
16. 发起一个知识库无法回答的问题，产生 missed 记录。
17. 登录 ADMIN。
18. 查询 admin chat records。
19. 查询 admin chat missed。
20. 使用另一个普通用户 B 验证越权访问失败。

## 12. 第二周验收结论模板

验收时建议按以下格式记录：

```text
验收日期：
验收环境：
代码版本：
数据库版本：
AI 模式：MOCK / REAL

必须通过项：
- 文件上传/存储：
- 文件解析/切片：
- parse 任务闭环：
- embed 状态闭环：
- 检索：
- 问答：
- 记录/反馈/管理查询：
- 权限隔离：
- 数据一致性：

可接受但需后续优化项：
- 

阻塞问题：
- 

最终结论：
通过 / 有条件通过 / 不通过
```

## 13. 第二周通过标准

第二周可以判定为通过，必须同时满足：

- 真实 txt/md 文件可以上传并保存。
- 真实文件可以解析并切片。
- parse 任务有可查询状态。
- embed 状态可查询。
- 检索接口可返回相关 chunk；如果真实向量检索未完成，必须明确标注 mock。
- 问答接口可返回答案和 citations；如果真实 LLM 未完成，必须明确标注 placeholder。
- 问答记录、反馈、管理日志、未命中问题全部继续可用。
- 普通用户数据隔离有效。
- 管理端 ADMIN 权限有效。
- 数据库中能追踪完整主链路。

第二周不应因为以下事项未完成而判定失败：

- 暂不支持 PDF/DOC/DOCX 真实解析。
- 暂不支持 OCR。
- 暂不支持流式输出。
- 暂不支持完整 conversation/message 模型。
- 暂不支持复杂 RBAC。
- 暂不支持复杂统计看板。

但如果真实 embedding、真实向量检索或真实 LLM 未完成，验收结论只能写为“有条件通过”，不能写成完整真实 RAG MVP 通过。
