# 面试准备指南

## 1. 30 秒项目介绍

这是一个 AI 知识库问答平台，我主要做 Java 后端。用户可以创建知识库、上传 txt / md 文档，系统会解析切片、生成 embedding，并在提问时通过 RAG 检索相关 chunk，再调用大模型生成答案。项目还做了引用溯源、多轮会话、拒答策略、用户反馈和管理端基础统计。当前定位是 MVP 级项目，向量存在 MySQL，Java 侧做相似度检索，重点是完整链路和工程闭环。

## 2. 2 分钟项目介绍

项目使用 Spring Boot、Spring Security、JWT、MyBatis-Plus、MySQL 和 Flyway。核心流程是：用户登录后创建知识库，上传文档；文档经过 process 解析和切片，生成 `document_chunk`；再执行 embed，把每个 chunk 调用 Embedding API 生成向量并存入 `chunk_embedding`；用户提问时，系统对问题生成 query embedding，在当前知识库中检索相关 chunk，然后根据有效命中情况决定生成答案或拒答。

我重点做了几个闭环：第一是 RAG 质量闭环，区分 raw 检索结果和 effective 有效结果，增加 `NO_HIT`、`WEAK_HIT`、`LLM_UNAVAILABLE` 等状态，并保存 citations。第二是会话系统，使用 `conversation` 和 `message` 承载多轮上下文，同时保留 `chat_record` 做审计和运营分析。第三是文档生命周期，支持重处理、重新向量化、状态聚合和删除关联数据。第四是管理端基础运营，能查看未命中问题、用户反馈、热门问题和基础统计。

## 3. 5 分钟技术深挖讲解主线

1. 先讲业务目标：做一个能基于用户知识库回答问题的平台。
2. 再讲整体架构：认证、知识库、文档、Embedding、RAG、会话、反馈、管理端。
3. 讲文档链路：上传、process、chunk、embed、状态和删除。
4. 讲 RAG 链路：query embedding、相似度检索、raw/effective、拒答、LLM、AnswerExtractor、citations。
5. 讲会话链路：conversation/message/chat_record 分工，多轮上下文加载。
6. 讲工程化：Flyway、JWT、管理员权限、API Key 环境变量、Debug 接口保护。
7. 最后讲不足和优化：当前 MySQL + Java cosine 是 MVP，生产可换向量库、异步任务、混合检索、rerank。

## 4. 项目架构讲解

可以按模块讲：

- 认证层：`/auth/register`、`/auth/login`，登录后用 JWT 访问接口。
- 知识库层：`knowledge_base` 绑定用户，作为文档和问答的隔离边界。
- 文档层：`document` 存文件元信息和生命周期状态。
- 切片层：`document_chunk` 存文本片段。
- 向量层：`chunk_embedding` 存 embedding 状态和向量 JSON。
- 问答层：`chat_record` 存一次问答的完整追溯。
- 会话层：`conversation` 和 `message` 支持多轮上下文。
- 运营层：`chat_feedback` 和管理端统计接口用于质量闭环。

## 5. RAG 链路讲解

RAG 链路分两段：离线准备和在线问答。

离线准备是文档上传后执行 process 和 embed，把文档变成 chunk，再把 chunk 变成向量。在线问答时，对用户问题生成 query embedding，在当前知识库中检索相似 chunk。检索结果先得到 raw candidates，再按分数阈值过滤成 effective chunks。如果没有 raw，就是 `NO_HIT`；如果 raw 有但 effective 为空，就是 `WEAK_HIT`；只有 effective 非空才调用 LLM 生成答案，并把 citations 返回给用户。

## 6. 文档处理链路讲解

上传文档后，系统先保存 `document` 记录和本地文件路径。执行 process 时解析文本并切片，写入 `document_chunk`，同时更新文档状态。执行 embed 时遍历 chunk，调用 Embedding API，结果写入 `chunk_embedding.vector_json`。如果文档内容或切片参数变了，可以用 `process force=true` 清理旧 chunk 和旧 embedding 后重建；如果向量模型或向量结果需要刷新，可以用 `embed force=true` 重新生成。

## 7. 会话系统讲解

`chat_record` 更像问答审计表，适合记录一次问答的检索数量、命中状态、引用等信息。但多轮对话需要按时间顺序加载 user / assistant 消息，所以单独设计了 `conversation` 和 `message`。`conversation` 存会话聚合信息，`message` 存每条消息。用户追问时传入 `conversationId`，系统加载最近几条消息作为上下文，再进行 RAG 问答。

## 8. 管理端运营讲解

管理端不是完整后台页面，而是一组基础运营接口。管理员可以看问答详情、未命中问题、用户反馈、热门问题和基础统计。这些数据能帮助判断知识库是否缺内容、哪些答案用户不满意、哪些问题最常被问，然后反过来指导补充文档和重新 embedding。

## 9. 安全与权限讲解

项目使用 Spring Security 和 JWT。用户登录后请求携带 Bearer token，服务层通过当前用户 ID 校验知识库、文档、会话和问答记录是否属于本人。管理端接口通过 `AdminPermissionService` 校验管理员角色。外部 LLM / Embedding 的 API Key 通过环境变量配置，不应写入代码仓库。Debug 接口只用于本地联调，生产环境应关闭或严格限制。

## 10. 异常处理讲解

异常分几类：

- 参数错误：通过 Bean Validation 和业务校验返回错误。
- 资源不存在 / 无权限：对外通常返回资源不存在或权限错误，避免越权访问。
- 文档状态错误：比如未切片不能 embedding，正在处理不能重复提交。
- RAG 质量异常：无命中、弱命中、检索不可用、LLM 不可用都写入 `answerStatus`。
- 外部 API 异常：不会返回密钥，错误信息会做截断和脱敏。

## 11. 数据库设计讲解

核心表包括：

- `user`：用户和角色。
- `knowledge_base`：知识库。
- `document`：文档元信息和生命周期状态。
- `document_chunk`：文档切片。
- `chunk_embedding`：chunk 的向量和向量化状态。
- `chat_record`：单次问答记录和 RAG 追溯字段。
- `conversation`：会话聚合。
- `message`：会话消息。
- `chat_feedback`：用户反馈。
- `task_record`：文档处理和向量化任务记录。

## 12. 高频追问与回答思路

### 为什么做这个项目？

想做一个比普通 CRUD 更完整的 Java 后端项目，把用户认证、文档处理、数据库设计、外部 API、RAG 和运营统计串起来，同时也符合现在 AI 应用落地的方向。

### 为什么用 RAG？

直接问大模型容易产生无依据回答。RAG 先从用户自己的知识库检索相关片段，再让模型基于片段回答，可以让答案更贴近业务文档，并通过 citations 做追溯。

### embedding 是什么？

embedding 是把文本转换成一组数字向量，使语义相近的文本在向量空间中距离更近。这样用户问题和文档 chunk 就能通过相似度进行匹配。

### 文档如何变成向量？

文档先解析成纯文本，再按 chunkSize 和 overlap 切片。每个 chunk 调用 Embedding API 生成向量，最后把向量 JSON、模型名和状态写入 `chunk_embedding`。

### 向量怎么存？

当前 MVP 中存在 MySQL 的 `chunk_embedding.vector_json` 字段。这样实现简单，方便演示和调试，但不适合大规模生产检索。

### 如何检索？

用户问题先生成 query embedding，然后从当前知识库的成功 embedding 中取候选向量，Java 侧计算相似度并按分数排序，再按阈值筛出 effective chunks。

### 如何降低大模型幻觉？

主要靠三点：只把检索到的有效 chunk 放进 prompt；无命中或弱命中时直接拒答；返回 citations，让答案有来源可追溯。

### citations 怎么实现？

把参与回答的 effective chunks 转成 `CitationVO`，包括 chunkId、documentId、chunkIndex、documentName、score 和文本片段，然后序列化到 `chat_record.citations_json`。

### 没有命中文档怎么办？

如果 raw 结果为空，返回 `NO_HIT`；如果只有弱相关结果，返回 `WEAK_HIT`。两种情况都不会让 LLM 硬编答案。

### 多轮对话怎么做？

每次问答属于一个 conversation。系统把用户问题和助手回答分别写入 message，追问时根据 conversationId 加载最近消息作为上下文。

### conversation 和 chat_record 有什么区别？

`conversation` 是会话聚合，`message` 是上下文消息；`chat_record` 是一次问答的审计和运营记录，保存命中状态、检索数量和 citations。

### 文档删除为什么要删 chunk 和 embedding？

如果只删 document，不删 chunk 和 embedding，检索时可能还会命中过期内容，也会产生孤立数据，所以删除时要先清理关联数据。

### force=true 是为了解决什么问题？

`process force=true` 用于文档内容或切片参数变化时重建 chunk；`embed force=true` 用于模型变化或向量异常时重新生成向量。它们都需要清理旧数据，避免新旧数据混用。

### 管理端统计有什么价值？

它能发现未命中问题、差评答案和热门问题，帮助判断知识库缺什么内容，以及 RAG 质量是否需要优化。

### 权限怎么保证？

普通用户只能访问自己拥有的知识库、文档、会话和问答记录。管理员接口额外校验角色。接口层用 JWT 识别用户，服务层做资源归属校验。

### API Key 怎么保护？

API Key 通过环境变量注入，不写入代码仓库。错误信息和日志中不应打印完整密钥，Debug 接口只用于本地环境。

### 当前项目最大的不足是什么？

当前是 MVP 级 RAG，向量存在 MySQL，Java 侧做相似度检索；文档处理是同步链路；没有专业向量库、rerank、异步队列和完整前端后台。

### 如果上线生产，你会怎么优化？

接入向量数据库或向量索引，增加异步任务队列处理文档和 embedding，引入混合检索与 rerank，增加监控和评估集，完善权限粒度、文件存储和管理端处理流程。
