# 当前系统接口总览

> 依据当前 Controller 提取。权限说明以 Spring Security 和服务层校验为准；管理员接口需管理员角色。若后续路径调整，以 Controller 为准。

| 模块 | 请求方法 | 接口路径 | 功能说明 | 权限要求 | 是否核心展示接口 | 当前状态 |
|---|---|---|---|---|---|---|
| Auth | POST | `/auth/register` | 用户注册 | 无需登录 | 是 | 已实现 |
| Auth | POST | `/auth/login` | 用户登录并返回 JWT | 无需登录 | 是 | 已实现 |
| KnowledgeBase | POST | `/api/kb` | 创建知识库 | Bearer token | 是 | 已实现 |
| KnowledgeBase | GET | `/api/kb` | 分页查询当前用户知识库 | Bearer token | 是 | 已实现 |
| KnowledgeBase | GET | `/api/kb/{id}` | 查询知识库详情 | Bearer token，限本人 | 是 | 已实现 |
| KnowledgeBase | GET | `/api/kb/{id}/documents` | 查询知识库下文档 | Bearer token，限本人 | 是 | 已实现 |
| Document | POST | `/api/documents/upload` | 上传文档元数据 | Bearer token，限本人知识库 | 否 | 已实现 |
| Document | POST | `/api/documents/upload-file` | 上传 txt / md 真实文件 | Bearer token，限本人知识库 | 是 | 已实现 |
| Document | GET | `/api/documents` | 分页查询文档 | Bearer token | 是 | 已实现 |
| Document | GET | `/api/documents/{documentId}` | 查询文档详情 | Bearer token，限本人 | 是 | 已实现 |
| Document | DELETE | `/api/documents/{documentId}` | 删除文档及关联数据 | Bearer token，限本人 | 是 | 已实现 |
| Document | GET | `/api/documents/{documentId}/status` | 查询文档生命周期状态 | Bearer token，限本人 | 是 | 已实现 |
| Process | POST | `/api/documents/{documentId}/parse` | 创建解析任务占位 | Bearer token，限本人 | 否 | 已实现 |
| Process | POST | `/api/documents/{documentId}/process` | 执行文档解析 / 切片，支持 `force` | Bearer token，限本人 | 是 | 已实现 |
| Process | GET | `/api/documents/{documentId}/chunks` | 查询文档 chunk | Bearer token，限本人 | 是 | 已实现 |
| Process | GET | `/api/documents/{documentId}/tasks` | 查询文档任务记录 | Bearer token，限本人 | 否 | 已实现 |
| Embedding | POST | `/api/documents/{documentId}/embed` | 对 chunk 执行 embedding，支持 `force` | Bearer token，限本人 | 是 | 已实现 |
| Embedding | GET | `/api/documents/{documentId}/embedding-status` | 查询 embedding 明细状态 | Bearer token，限本人 | 是 | 已实现 |
| Retrieval | POST | `/api/retrieval/search` | 只检索 chunk，不生成答案 | Bearer token，限本人知识库 | 否 | 已实现 |
| Chat | POST | `/api/chat/ask` | RAG 问答主接口 | Bearer token，限本人知识库 | 是 | 已实现 |
| Conversation | GET | `/api/chat/conversations` | 会话列表 | Bearer token | 是 | 已实现 |
| Conversation | GET | `/api/chat/conversations/{conversationId}` | 会话详情和消息列表 | Bearer token，限本人 | 是 | 已实现 |
| Chat Record | GET | `/api/chat/records` | 用户问答记录分页 | Bearer token | 是 | 已实现 |
| Chat Record | GET | `/api/chat/records/{id}` | 用户问答详情 | Bearer token，限本人 | 是 | 已实现 |
| Feedback | POST | `/api/chat/records/{id}/feedback` | 提交 LIKE / DISLIKE 反馈 | Bearer token，限本人记录 | 是 | 已实现 |
| Admin Chat | GET | `/api/admin/chat/records` | 管理端问答记录分页 | Bearer token，管理员 | 是 | 已实现 |
| Admin Chat | GET | `/api/admin/chat/records/{id}` | 管理端问答详情 | Bearer token，管理员 | 是 | 已实现 |
| Admin Operation | GET | `/api/admin/chat/missed-questions` | 未命中问题列表 | Bearer token，管理员 | 是 | 已实现 |
| Admin Operation | GET | `/api/admin/chat/feedback` | 反馈运营列表 | Bearer token，管理员 | 是 | 已实现 |
| Admin Operation | GET | `/api/admin/chat/hot-questions` | 热门问题 TopN | Bearer token，管理员 | 是 | 已实现 |
| Admin Operation | GET | `/api/admin/chat/stats` | 问答和反馈基础统计 | Bearer token，管理员 | 是 | 已实现 |
| Task | GET | `/api/tasks/{id}` | 查询任务详情 | Bearer token，限有权限任务 | 否 | 已实现 |
| System | GET | `/api/system/runtime-mode` | 查看 AI 运行模式，不返回密钥 | Bearer token，管理员 | 否 | 已实现 |
| Debug | POST | `/debug/ai/embedding` | 调试 Embedding 外部能力 | 本地 / 显式开关，需以配置为准 | 否 | 已实现，生产应关闭 |
| Debug | POST | `/debug/ai/llm` | 调试 LLM 外部能力 | 本地 / 显式开关，需以配置为准 | 否 | 已实现，生产应关闭 |

## Debug 接口风险

`/debug/ai/*` 用于本地联调外部 AI 能力，虽然代码中有 `AiDebugAccessGuard`，但投简历前仍应确认：

- 生产配置默认关闭。
- 不返回真实 API Key。
- 日志不打印完整 Authorization Header。
- README 明确说明仅用于本地调试。
