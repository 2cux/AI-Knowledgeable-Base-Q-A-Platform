# 第二周 API 清单

## 1. 文档定位

本文档基于当前项目的真实开发进度，整理第二周需要保留、新增和调整的后端 API。

第二周 API 设计目标不是扩展大量新业务模块，而是服务于 MVP 主链路真实可用：

```text
用户认证
  -> 创建知识库
  -> 上传真实文件
  -> 解析文件并切片
  -> 执行 embedding
  -> 检索知识库 chunk
  -> 调用 LLM 问答
  -> 保存问答记录、反馈和未命中问题
```

接口优先级说明：

- 必须：第二周必须完成或保持可用，直接影响 MVP 主链路。
- 可选：时间允许再做，不阻塞第二周 MVP 验收。

端类型说明：

- 用户端：普通登录用户使用。
- 管理端：ADMIN 用户使用。
- 系统/调试：用于开发、验收、任务调试或内部触发。

## 2. 第二周接口总览

### 2.1 保留不变的接口

这些接口当前已有实现，第二周原则上不改路径，不破坏兼容。

| 模块 | 方法 | 路径 | 端类型 | 优先级 |
| --- | --- | --- | --- | --- |
| 认证 | POST | `/auth/register` | 用户端 | 必须 |
| 认证 | POST | `/auth/login` | 用户端 | 必须 |
| 知识库 | POST | `/api/kb` | 用户端 | 必须 |
| 知识库 | GET | `/api/kb` | 用户端 | 必须 |
| 知识库 | GET | `/api/kb/{id}` | 用户端 | 必须 |
| 文档 | GET | `/api/documents` | 用户端 | 必须 |
| 文档 | GET | `/api/documents/{id}` | 用户端 | 必须 |
| 文档 | GET | `/api/documents/{id}/chunks` | 用户端 | 必须 |
| 文档向量化 | GET | `/api/documents/{id}/embedding-status` | 用户端 | 必须 |
| 检索 | POST | `/api/retrieval/search` | 用户端 | 必须 |
| 问答 | POST | `/api/chat/ask` | 用户端 | 必须 |
| 问答记录 | GET | `/api/chat/records` | 用户端 | 必须 |
| 问答记录 | GET | `/api/chat/records/{id}` | 用户端 | 必须 |
| 反馈 | POST | `/api/chat/records/{id}/feedback` | 用户端 | 必须 |
| 管理日志 | GET | `/api/admin/chat/records` | 管理端 | 必须 |
| 管理日志 | GET | `/api/admin/chat/records/{id}` | 管理端 | 必须 |
| 未命中问题 | GET | `/api/admin/chat/missed` | 管理端 | 必须 |

### 2.2 需要新增的接口

| 模块 | 方法 | 路径 | 端类型 | 优先级 |
| --- | --- | --- | --- | --- |
| 文件上传 | POST | `/api/documents/upload-file` | 用户端 | 必须 |
| 文档任务 | GET | `/api/documents/{id}/tasks` | 用户端 | 必须 |
| 任务查询 | GET | `/api/tasks/{id}` | 用户端 | 必须 |
| 文档解析执行 | POST | `/api/documents/{id}/parse/run` | 用户端/调试 | 必须 |
| 文档重建索引 | POST | `/api/documents/{id}/index` | 用户端 | 可选 |
| 系统能力 | GET | `/api/system/ai-capabilities` | 系统/调试 | 可选 |

### 2.3 需要修改请求或响应结构的接口

| 模块 | 方法 | 路径 | 调整点 | 优先级 |
| --- | --- | --- | --- | --- |
| 文档元数据上传 | POST | `/api/documents/upload` | 标记为元数据上传，真实文件走新接口 | 必须 |
| 文档解析任务 | POST | `/api/documents/{id}/parse` | 返回任务详情，不只返回 taskId | 必须 |
| 文档处理 | POST | `/api/documents/{id}/process` | 支持不传 textContent 时从 storagePath 解析 | 必须 |
| 文档向量化 | POST | `/api/documents/{id}/embed` | 返回任务/执行状态，支持真实 embedding 模型 | 必须 |
| 检索 | POST | `/api/retrieval/search` | 返回真实向量检索 score、model、retrievalMode | 必须 |
| 问答 | POST | `/api/chat/ask` | 返回 answerMode、model、citations、conversationId | 必须 |
| 问答详情 | GET | `/api/chat/records/{id}` | 补充 conversationId、citations、topK | 必须 |
| 管理详情 | GET | `/api/admin/chat/records/{id}` | 补充 citations、userId、topK | 必须 |

## 3. 保留不变的接口明细

### 3.1 用户注册

接口：`POST /auth/register`

端类型：用户端

优先级：必须

用途：

- 创建普通用户账号。

关键请求字段：

- `username`：用户名。
- `password`：密码。
- `confirmPassword`：确认密码。
- `email`：邮箱，可选。

关键返回字段：

- `data`：用户 ID。

说明：

- 第二周不调整路径。
- 第二周可选补强：注册时初始化 nickname 或明确 nickname 数据库默认值策略。

### 3.2 用户登录

接口：`POST /auth/login`

端类型：用户端

优先级：必须

用途：

- 用户登录并获取 JWT。

关键请求字段：

- `username`：用户名。
- `password`：密码。

关键返回字段：

- `token`：JWT。
- `userId`：用户 ID。
- `username`：用户名。

第二周建议：

- 登录时校验用户 `status` 是否启用。该项属于 P2，不阻塞真实 RAG 主链路。

### 3.3 创建知识库

接口：`POST /api/kb`

端类型：用户端

优先级：必须

用途：

- 当前登录用户创建知识库。

关键请求字段：

- `name`：知识库名称。
- `description`：知识库描述，可选。

关键返回字段：

- `id`：知识库 ID。
- `name`：知识库名称。
- `description`：描述。
- `status`：状态。
- `createdAt`：创建时间。

### 3.4 查询知识库列表

接口：`GET /api/kb`

端类型：用户端

优先级：必须

用途：

- 分页查询当前用户拥有的知识库。

关键请求参数：

- `pageNum`：页码，默认 1。
- `pageSize`：每页数量，默认 10。

关键返回字段：

- `list`：知识库列表。
- `total`：总数。
- `pageNum`：当前页。
- `pageSize`：每页数量。

### 3.5 查询知识库详情

接口：`GET /api/kb/{id}`

端类型：用户端

优先级：必须

用途：

- 查询当前用户可访问的知识库详情。

关键路径参数：

- `id`：知识库 ID。

关键返回字段：

- `id`
- `name`
- `description`
- `status`
- `createdAt`
- `updatedAt`

### 3.6 查询文档列表

接口：`GET /api/documents`

端类型：用户端

优先级：必须

用途：

- 分页查询当前用户可访问的文档。

关键请求参数：

- `knowledgeBaseId`：知识库 ID，可选。
- `pageNum`：页码。
- `pageSize`：每页数量。

关键返回字段：

- `list[].id`
- `list[].knowledgeBaseId`
- `list[].fileName`
- `list[].fileType`
- `list[].fileSize`
- `list[].parseStatus`
- `total`

第二周建议：

- 返回字段可补充 `storagePath`、`taskStatus`、`lastTaskId`，用于前端展示文档处理状态。

### 3.7 查询文档详情

接口：`GET /api/documents/{id}`

端类型：用户端

优先级：必须

用途：

- 查询单个文档详情。

关键路径参数：

- `id`：文档 ID。

关键返回字段：

- `id`
- `knowledgeBaseId`
- `fileName`
- `fileType`
- `fileSize`
- `storagePath`
- `parseStatus`
- `createdBy`
- `createdAt`
- `updatedAt`

第二周建议：

- 补充最近任务状态、错误信息、chunkCount、embeddingStatus。

### 3.8 查询文档切片

接口：`GET /api/documents/{id}/chunks`

端类型：用户端

优先级：必须

用途：

- 查询指定文档的 chunk 列表。

关键路径参数：

- `id`：文档 ID。

关键返回字段：

- `chunkId`
- `documentId`
- `knowledgeBaseId`
- `chunkIndex`
- `content`
- `tokenCount`
- `createdAt`

### 3.9 查询文档向量化状态

接口：`GET /api/documents/{id}/embedding-status`

端类型：用户端

优先级：必须

用途：

- 查询文档下全部 chunk 的 embedding 状态。

关键路径参数：

- `id`：文档 ID。

关键返回字段：

- `documentId`
- `knowledgeBaseId`
- `totalChunks`
- `successCount`
- `failedCount`
- `pendingCount`
- `status`
- `chunks[].chunkId`
- `chunks[].embeddingModel`
- `chunks[].vectorId`
- `chunks[].status`
- `chunks[].embeddingError`

### 3.10 检索知识库 chunk

接口：`POST /api/retrieval/search`

端类型：用户端

优先级：必须

用途：

- 根据问题在指定知识库内检索相关 chunk。

关键请求字段：

- `knowledgeBaseId`：知识库 ID。
- `question`：问题。
- `topK`：返回数量，可选。

关键返回字段：

- `knowledgeBaseId`
- `question`
- `topK`
- `total`
- `chunks[].chunkId`
- `chunks[].documentId`
- `chunks[].documentName`
- `chunks[].chunkIndex`
- `chunks[].content`
- `chunks[].score`

第二周调整：

- 增加 `retrievalMode`：`MOCK` 或 `VECTOR`。
- 增加 `embeddingModel`。
- score 由真实向量检索返回时，应保持语义一致。

### 3.11 发起问答

接口：`POST /api/chat/ask`

端类型：用户端

优先级：必须

用途：

- 用户基于指定知识库提问。

关键请求字段：

- `knowledgeBaseId`：知识库 ID。
- `question`：问题。
- `topK`：检索数量，可选。
- `conversationId`：会话 ID，可选。

关键返回字段：

- `conversationId`
- `answer`
- `matched`
- `retrievedChunkCount`
- `citations`

第二周调整：

- 增加 `answerMode`：`PLACEHOLDER` 或 `LLM`。
- 增加 `model`：LLM 模型名称。
- 增加 `retrievalMode`：`MOCK` 或 `VECTOR`。
- 未命中时必须明确返回知识不足类答案，不允许编造。

### 3.12 查询问答记录列表

接口：`GET /api/chat/records`

端类型：用户端

优先级：必须

用途：

- 当前用户查询自己的问答历史。

关键请求参数：

- `knowledgeBaseId`：知识库 ID，可选。
- `pageNum`：页码。
- `pageSize`：每页数量。

关键返回字段：

- `list[].id`
- `list[].knowledgeBaseId`
- `list[].question`
- `list[].answerPreview`
- `list[].matched`
- `list[].retrievedChunkCount`
- `list[].createdAt`
- `total`

第二周建议：

- 列表可补充 `conversationId`，方便按会话归类。

### 3.13 查询问答记录详情

接口：`GET /api/chat/records/{id}`

端类型：用户端

优先级：必须

用途：

- 当前用户查询自己的单条问答详情。

关键路径参数：

- `id`：问答记录 ID。

当前关键返回字段：

- `id`
- `knowledgeBaseId`
- `question`
- `answer`
- `matched`
- `retrievedChunkCount`
- `createdAt`

第二周必须补充：

- `conversationId`
- `topK`
- `citations`
- `answerMode`，如果落库或可推导。
- `model`，如果落库或可推导。

### 3.14 提交问答反馈

接口：`POST /api/chat/records/{id}/feedback`

端类型：用户端

优先级：必须

用途：

- 当前用户对自己的问答记录提交反馈。

关键路径参数：

- `id`：问答记录 ID。

关键请求字段：

- `feedbackType`：`LIKE` 或 `DISLIKE`。
- `comment`：备注，可选。

关键返回字段：

- `data`：通常为空。

### 3.15 管理端查询问答日志

接口：`GET /api/admin/chat/records`

端类型：管理端

优先级：必须

用途：

- 管理员分页查看全量问答记录。

关键请求参数：

- `knowledgeBaseId`：知识库 ID，可选。
- `matched`：是否命中，可选。
- `pageNum`
- `pageSize`

关键返回字段：

- `list[].id`
- `list[].knowledgeBaseId`
- `list[].question`
- `list[].answerPreview`
- `list[].matched`
- `list[].retrievedChunkCount`
- `list[].createdAt`
- `total`

第二周建议：

- 补充 `userId`、`conversationId`、`answerMode`。

### 3.16 管理端查询问答日志详情

接口：`GET /api/admin/chat/records/{id}`

端类型：管理端

优先级：必须

用途：

- 管理员查看单条问答记录详情。

关键路径参数：

- `id`：问答记录 ID。

当前关键返回字段：

- `id`
- `knowledgeBaseId`
- `conversationId`
- `question`
- `answer`
- `matched`
- `retrievedChunkCount`
- `createdAt`

第二周必须补充：

- `userId`
- `topK`
- `citations`
- `answerMode`
- `model`

### 3.17 管理端查询未命中问题

接口：`GET /api/admin/chat/missed`

端类型：管理端

优先级：必须

用途：

- 管理员查看检索未命中的问题，用于补充知识库。

关键请求参数：

- `knowledgeBaseId`：知识库 ID，可选。
- `pageNum`
- `pageSize`

关键返回字段：

- `list[].id`
- `list[].knowledgeBaseId`
- `list[].question`
- `list[].retrievedChunkCount`
- `list[].createdAt`
- `total`

第二周建议：

- 可选补充 `conversationId`、`userId`、`answerPreview`。

## 4. 需要新增的接口明细

### 4.1 上传真实文件

接口：`POST /api/documents/upload-file`

端类型：用户端

优先级：必须

用途：

- 上传真实文件并创建文档记录。
- 替代当前仅保存元数据的上传方式，作为第二周真实文档接入主入口。

请求类型：

- `multipart/form-data`

关键请求参数：

- `knowledgeBaseId`：知识库 ID，必填。
- `file`：文件，必填。
- `fileName`：自定义文件名，可选；不传时使用原始文件名。

关键返回字段：

- `id`：文档 ID。
- `knowledgeBaseId`：知识库 ID。
- `fileName`：文件名。
- `fileType`：文件类型。
- `fileSize`：文件大小。
- `storagePath`：后端真实存储路径。
- `parseStatus`：初始状态，建议为 `UPLOADED`。
- `createdAt`

状态说明：

- 成功上传后只代表文件保存成功，不代表已解析。
- 后续解析由 `/api/documents/{id}/parse` 或 `/api/documents/{id}/parse/run` 触发。

### 4.2 查询文档任务列表

接口：`GET /api/documents/{id}/tasks`

端类型：用户端

优先级：必须

用途：

- 查询指定文档关联的处理任务。
- 用于确认解析、切片、embedding 等任务状态。

关键路径参数：

- `id`：文档 ID。

关键请求参数：

- `taskType`：任务类型，可选，例如 `DOCUMENT_PARSE`、`DOCUMENT_EMBEDDING`。
- `pageNum`
- `pageSize`

关键返回字段：

- `list[].id`
- `list[].taskType`
- `list[].bizType`
- `list[].bizId`
- `list[].status`
- `list[].errorMessage`
- `list[].retryCount`
- `list[].createdAt`
- `list[].updatedAt`
- `total`

### 4.3 查询任务详情

接口：`GET /api/tasks/{id}`

端类型：用户端

优先级：必须

用途：

- 查询单个任务状态。
- 第二周主要用于文档解析任务验收。

关键路径参数：

- `id`：任务 ID。

关键返回字段：

- `id`
- `taskType`
- `bizType`
- `bizId`
- `status`
- `errorMessage`
- `retryCount`
- `createdAt`
- `updatedAt`

权限说明：

- 用户只能查询自己可访问文档关联的任务。
- 管理员可通过管理端扩展接口查询全量任务，第二周可不做。

### 4.4 创建并执行文档解析任务

接口：`POST /api/documents/{id}/parse/run`

端类型：用户端/调试

优先级：必须

用途：

- 创建解析任务并立即执行。
- 适合第二周 MVP 验收，避免只创建任务但没有执行闭环。

关键路径参数：

- `id`：文档 ID。

关键请求字段：

- `chunkSize`：切片长度，可选，默认 500。
- `overlap`：重叠长度，可选，默认 50。
- `force`：是否强制重建切片，可选，默认 false。

关键返回字段：

- `taskId`
- `documentId`
- `knowledgeBaseId`
- `taskStatus`
- `parseStatus`
- `chunkCount`
- `errorMessage`

说明：

- 该接口可以在第二周作为同步执行入口。
- 后续如果改为异步执行，返回 `taskId` 后由 `/api/tasks/{id}` 查询状态。

### 4.5 重建文档向量索引

接口：`POST /api/documents/{id}/index`

端类型：用户端

优先级：可选

用途：

- 对指定文档执行 embedding 并写入真实向量索引。
- 可以作为 `/api/documents/{id}/embed` 的升级版或别名。

关键路径参数：

- `id`：文档 ID。

关键请求字段：

- `embeddingModel`：embedding 模型，可选。
- `force`：是否强制重建已有向量，可选。

关键返回字段：

- `taskId`
- `documentId`
- `knowledgeBaseId`
- `total`
- `successCount`
- `failedCount`
- `embeddingModel`
- `indexStatus`

说明：

- 如果第二周时间有限，可以先继续使用 `/api/documents/{id}/embed`，不新增该接口。

### 4.6 查询 AI 能力配置

接口：`GET /api/system/ai-capabilities`

端类型：系统/调试

优先级：可选

用途：

- 查看当前运行环境使用 mock 还是真实 AI 能力。

关键返回字段：

- `embeddingMode`：`MOCK` 或 `REAL`。
- `embeddingModel`
- `retrievalMode`：`MOCK` 或 `VECTOR`。
- `answerMode`：`PLACEHOLDER` 或 `LLM`。
- `llmModel`
- `vectorStoreType`

说明：

- 该接口方便验收和排障，不属于用户核心功能。

## 5. 需要修改的接口明细

### 5.1 文档元数据上传

接口：`POST /api/documents/upload`

端类型：用户端

优先级：必须

当前用途：

- 保存文档元数据。

第二周调整：

- 保留接口，避免破坏已有调用。
- 明确该接口只做元数据登记，不代表真实文件已经上传。
- 推荐真实文件接入使用 `/api/documents/upload-file`。

关键请求字段：

- `knowledgeBaseId`
- `fileName`
- `fileType`
- `fileSize`
- `storagePath`

关键返回字段：

- `id`
- `knowledgeBaseId`
- `fileName`
- `fileType`
- `fileSize`
- `storagePath`
- `parseStatus`

### 5.2 创建文档解析任务

接口：`POST /api/documents/{id}/parse`

端类型：用户端

优先级：必须

当前用途：

- 创建解析任务，当前返回 taskId。

第二周调整：

- 返回任务详情，而不是只返回 Long。
- 不一定立即执行；如果要立即执行，使用 `/api/documents/{id}/parse/run`。

关键返回字段建议：

- `taskId`
- `documentId`
- `taskType`
- `status`
- `parseStatus`
- `createdAt`

### 5.3 执行文档处理

接口：`POST /api/documents/{id}/process`

端类型：用户端

优先级：必须

当前用途：

- 传入 textContent 后执行切片。

第二周调整：

- 支持不传 `textContent` 时，从 document.storagePath 读取真实文件并解析。
- `textContent` 继续保留为调试入口。
- 解析失败时更新文档状态和任务状态。

关键请求字段：

- `chunkSize`
- `overlap`
- `textContent`：可选。

关键返回字段：

- `documentId`
- `knowledgeBaseId`
- `chunkCount`
- `parseStatus`
- `taskId`：建议新增。

### 5.4 执行文档向量化

接口：`POST /api/documents/{id}/embed`

端类型：用户端

优先级：必须

当前用途：

- 对文档下全部 chunk 执行 mock embedding。

第二周调整：

- 支持真实 embedding。
- 支持真实向量索引写入。
- 返回执行模式和状态。

关键请求字段：

- `embeddingModel`
- `force`：建议新增，可选。

关键返回字段：

- `documentId`
- `knowledgeBaseId`
- `total`
- `successCount`
- `failedCount`
- `embeddingModel`
- `embeddingMode`：`MOCK` 或 `REAL`。
- `indexStatus`：建议新增。

### 5.5 检索接口

接口：`POST /api/retrieval/search`

端类型：用户端

优先级：必须

第二周调整：

- 从 mock 检索逐步升级为真实向量检索。
- 响应中明确当前检索模式。

关键请求字段：

- `knowledgeBaseId`
- `question`
- `topK`

关键返回字段：

- `retrievalMode`
- `embeddingModel`
- `chunks[].score`
- `chunks[].vectorId`：可选。
- `chunks[].documentName`

### 5.6 问答接口

接口：`POST /api/chat/ask`

端类型：用户端

优先级：必须

第二周调整：

- 从占位答案逐步升级为真实 LLM 答案。
- 保留 conversationId。
- 保留 citations。
- 未命中时返回明确知识不足提示。

关键请求字段：

- `knowledgeBaseId`
- `question`
- `topK`
- `conversationId`

关键返回字段：

- `conversationId`
- `answer`
- `matched`
- `retrievedChunkCount`
- `citations`
- `answerMode`
- `model`
- `retrievalMode`

### 5.7 用户问答详情

接口：`GET /api/chat/records/{id}`

端类型：用户端

优先级：必须

第二周调整：

- 补充完整引用来源和会话信息。

关键返回字段新增：

- `conversationId`
- `topK`
- `citations`

### 5.8 管理端问答详情

接口：`GET /api/admin/chat/records/{id}`

端类型：管理端

优先级：必须

第二周调整：

- 补充管理员排障需要的完整字段。

关键返回字段新增：

- `userId`
- `topK`
- `citations`
- `answerMode`
- `model`

## 6. 建议新增的状态查询接口

### 6.1 文档处理总状态

接口：`GET /api/documents/{id}/process-status`

端类型：用户端

优先级：可选

用途：

- 聚合展示文档解析、切片、embedding、索引状态。

关键返回字段：

- `documentId`
- `parseStatus`
- `chunkCount`
- `embeddingStatus`
- `totalChunks`
- `embeddedChunks`
- `failedChunks`
- `lastTaskId`
- `lastTaskStatus`
- `lastErrorMessage`

说明：

- 如果不新增该接口，可以先通过文档详情、任务详情、embedding-status 三个接口组合查询。

### 6.2 任务详情

接口：`GET /api/tasks/{id}`

端类型：用户端

优先级：必须

用途：

- 查询单个任务状态。

说明：

- 这是第二周建议必须新增的最小状态查询接口。

### 6.3 文档任务列表

接口：`GET /api/documents/{id}/tasks`

端类型：用户端

优先级：必须

用途：

- 查询文档关联任务列表。

说明：

- 用于展示文档处理历史和失败原因。

### 6.4 管理端任务列表

接口：`GET /api/admin/tasks`

端类型：管理端

优先级：可选

用途：

- 管理员查看系统任务执行情况。

关键请求参数：

- `taskType`
- `status`
- `bizType`
- `bizId`
- `pageNum`
- `pageSize`

关键返回字段：

- `list[].id`
- `list[].taskType`
- `list[].bizType`
- `list[].bizId`
- `list[].status`
- `list[].errorMessage`
- `list[].retryCount`
- `list[].createdAt`
- `list[].updatedAt`

## 7. 用户端接口汇总

第二周用户端必须保证以下接口可用：

- `POST /auth/register`
- `POST /auth/login`
- `POST /api/kb`
- `GET /api/kb`
- `GET /api/kb/{id}`
- `POST /api/documents/upload-file`
- `POST /api/documents/{id}/parse`
- `POST /api/documents/{id}/parse/run`
- `GET /api/documents`
- `GET /api/documents/{id}`
- `GET /api/documents/{id}/chunks`
- `GET /api/documents/{id}/tasks`
- `GET /api/tasks/{id}`
- `POST /api/documents/{id}/process`
- `POST /api/documents/{id}/embed`
- `GET /api/documents/{id}/embedding-status`
- `POST /api/retrieval/search`
- `POST /api/chat/ask`
- `GET /api/chat/records`
- `GET /api/chat/records/{id}`
- `POST /api/chat/records/{id}/feedback`

用户端可选接口：

- `POST /api/documents/{id}/index`
- `GET /api/documents/{id}/process-status`

## 8. 管理端接口汇总

第二周管理端必须保证以下接口可用：

- `GET /api/admin/chat/records`
- `GET /api/admin/chat/records/{id}`
- `GET /api/admin/chat/missed`

管理端可选接口：

- `GET /api/admin/tasks`

说明：

- 第二周不建议新增复杂管理端功能。
- 管理端重点服务于问答日志排查和未命中问题运营。

## 9. 第二周必须完成接口清单

### 9.1 必须保持可用

- `POST /auth/register`
- `POST /auth/login`
- `POST /api/kb`
- `GET /api/kb`
- `GET /api/kb/{id}`
- `GET /api/documents`
- `GET /api/documents/{id}`
- `GET /api/documents/{id}/chunks`
- `POST /api/documents/{id}/embed`
- `GET /api/documents/{id}/embedding-status`
- `POST /api/retrieval/search`
- `POST /api/chat/ask`
- `GET /api/chat/records`
- `GET /api/chat/records/{id}`
- `POST /api/chat/records/{id}/feedback`
- `GET /api/admin/chat/records`
- `GET /api/admin/chat/records/{id}`
- `GET /api/admin/chat/missed`

### 9.2 必须新增或调整

- `POST /api/documents/upload-file`
- `POST /api/documents/{id}/parse`
- `POST /api/documents/{id}/parse/run`
- `POST /api/documents/{id}/process`
- `GET /api/documents/{id}/tasks`
- `GET /api/tasks/{id}`
- `GET /api/chat/records/{id}` 补充 citations、conversationId、topK。
- `GET /api/admin/chat/records/{id}` 补充 citations、userId、topK。
- `POST /api/chat/ask` 补充 answerMode、model、retrievalMode。
- `POST /api/retrieval/search` 补充 retrievalMode、embeddingModel。

## 10. 第二周可选接口清单

- `POST /api/documents/{id}/index`
- `GET /api/documents/{id}/process-status`
- `GET /api/system/ai-capabilities`
- `GET /api/admin/tasks`

## 11. 第二周 API 验收路径

建议第二周按以下顺序验收 API：

1. `POST /auth/register`
2. `POST /auth/login`
3. `POST /api/kb`
4. `POST /api/documents/upload-file`
5. `POST /api/documents/{id}/parse/run`
6. `GET /api/documents/{id}/chunks`
7. `POST /api/documents/{id}/embed`
8. `GET /api/documents/{id}/embedding-status`
9. `POST /api/retrieval/search`
10. `POST /api/chat/ask`
11. `GET /api/chat/records/{id}`
12. `POST /api/chat/records/{id}/feedback`
13. `GET /api/admin/chat/records`
14. `GET /api/admin/chat/missed`

验收通过标准：

- 真实文件能上传并保存。
- 文档能从真实文件解析并切片。
- 任务状态能查询。
- chunk 能完成 embedding。
- 检索能返回相关 chunk。
- 问答能返回答案和 citations。
- 问答记录、反馈、管理日志、未命中问题均可查询。

## 12. 结论

第二周 API 的重点是补齐真实 MVP 主链路所需接口，不是扩大系统功能边界。

必须新增的核心接口只有三类：

- 真实文件上传：`POST /api/documents/upload-file`
- 任务状态查询：`GET /api/tasks/{id}`、`GET /api/documents/{id}/tasks`
- 解析执行闭环：`POST /api/documents/{id}/parse/run`

其余工作应优先通过增强现有接口完成，避免 API 数量膨胀。只要这些接口配合已有问答、检索、记录和管理日志接口稳定运行，第二周就可以完成“真实文档进入系统并产出可追溯问答”的 MVP 目标。
