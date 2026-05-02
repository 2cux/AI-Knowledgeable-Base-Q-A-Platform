# 最终功能清单

> 说明：本清单以当前代码为主要依据。无法从代码中完全确认的内容标注为“需以实际代码为准”。当前阶段目标是收尾、验收、包装和面试准备，不建议继续扩张大功能。

## 1. 用户模块

- 已完成功能：注册、登录、JWT 颁发、当前用户上下文读取。
- 关键接口：`POST /auth/register`、`POST /auth/login`。
- 关键表：`user`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：必须补齐 README 中注册 / 登录示例。
- 面试展示价值：能说明 Spring Security + JWT 的基础认证链路。

## 2. 知识库模块

- 已完成功能：创建知识库、分页查询、详情查询、查询知识库下文档。
- 关键接口：`POST /api/kb`、`GET /api/kb`、`GET /api/kb/{id}`、`GET /api/kb/{id}/documents`。
- 关键表：`knowledge_base`、`document`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：需要准备示例知识库数据。
- 面试展示价值：可讲用户数据隔离和后续 RAG 的业务入口。

## 3. 文档模块

- 已完成功能：元数据上传、真实文件上传、分页列表、详情、状态、删除。
- 关键接口：`POST /api/documents/upload`、`POST /api/documents/upload-file`、`GET /api/documents`、`GET /api/documents/{documentId}`、`GET /api/documents/{documentId}/status`、`DELETE /api/documents/{documentId}`。
- 关键表：`document`、`task_record`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：必须验证 txt / md 上传和删除链路。
- 面试展示价值：可讲文件元数据、真实文件存储、状态机和权限校验。

## 4. 文档解析 / 切片模块

- 已完成功能：`process` 执行文本解析与切片，支持 `chunkSize`、`overlap`、`textContent`、`force`；可查询 chunk。
- 关键接口：`POST /api/documents/{documentId}/process`、`GET /api/documents/{documentId}/chunks`。
- 关键表：`document_chunk`、`task_record`、`document`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：必须准备一个可稳定切片的 txt / md 示例。
- 面试展示价值：可讲文档如何从文件进入 RAG。

## 5. Embedding 模块

- 已完成功能：文档 chunk 向量化、向量 JSON 入库、状态查询、失败记录、`force=true` 重新向量化。
- 关键接口：`POST /api/documents/{documentId}/embed`、`GET /api/documents/{documentId}/embedding-status`、`POST /debug/ai/embedding`。
- 关键表：`chunk_embedding`、`document_chunk`、`document`、`task_record`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：必须检查 API Key 配置说明，避免密钥泄露。
- 面试展示价值：可讲 embedding 生成、存储、失败重试和状态聚合。

## 6. RAG 问答模块

- 已完成功能：知识库检索、有效 chunk 过滤、LLM 生成、答案落库、响应返回。
- 关键接口：`POST /api/chat/ask`、`POST /api/retrieval/search`。
- 关键表：`chat_record`、`chunk_embedding`、`document_chunk`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：必须用 Apifox 跑通一次完整问答。
- 面试展示价值：项目核心亮点，可讲 RAG 主链路。

## 7. 拒答策略模块

- 已完成功能：`NO_HIT`、`WEAK_HIT`、`LLM_UNAVAILABLE`、`RETRIEVAL_UNAVAILABLE` 状态；未命中或弱命中时不调用 / 不依赖 LLM 硬编答案。
- 关键接口：`POST /api/chat/ask`。
- 关键表：`chat_record.answer_status`、`chat_record.matched`、`chat_record.retrieved_chunk_count`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：需要准备未命中演示问题。
- 面试展示价值：可讲如何降低幻觉。

## 8. 会话系统模块

- 已完成功能：conversation / message 表；`conversationUid` 对外作为 `conversationId`；问答时创建或复用会话；会话列表和详情。
- 关键接口：`GET /api/chat/conversations`、`GET /api/chat/conversations/{conversationId}`、`POST /api/chat/ask`。
- 关键表：`conversation`、`message`、`chat_record`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：需要跑通同一 `conversationId` 的追问。
- 面试展示价值：可讲为什么 message 更适合多轮上下文。

## 9. 问答记录与追溯模块

- 已完成功能：用户问答记录列表、详情；保存 citations、命中状态、检索数量、topK。
- 关键接口：`GET /api/chat/records`、`GET /api/chat/records/{id}`。
- 关键表：`chat_record`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：需要补充 Apifox 验收截图或说明。
- 面试展示价值：可讲 RAG 可追溯性。

## 10. 文档生命周期模块

- 已完成功能：状态字段增强、状态聚合、`process force=true`、`embed force=true`、删除 document / chunk / embedding / task / file。
- 关键接口：`GET /api/documents/{documentId}/status`、`POST /api/documents/{documentId}/process`、`POST /api/documents/{documentId}/embed`、`DELETE /api/documents/{documentId}`。
- 关键表：`document`、`document_chunk`、`chunk_embedding`、`task_record`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：必须验证重处理、重新 embedding、删除。
- 面试展示价值：可讲数据一致性和可恢复设计。

## 11. 用户反馈模块

- 已完成功能：用户对自己的问答记录提交 `LIKE` / `DISLIKE` 和评论。
- 关键接口：`POST /api/chat/records/{id}/feedback`。
- 关键表：`chat_feedback`、`chat_record`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：需要验证重复提交策略，需以实际代码为准。
- 面试展示价值：可讲质量闭环和运营数据来源。

## 12. 管理端问答详情模块

- 已完成功能：管理员分页查看问答记录、按命中状态筛选、查看详情和 citations。
- 关键接口：`GET /api/admin/chat/records`、`GET /api/admin/chat/records/{id}`。
- 关键表：`chat_record`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：必须准备管理员账号或说明如何设置角色。
- 面试展示价值：可讲运营排障和权限控制。

## 13. 管理端未命中治理模块

- 已完成功能：分页查看 `matched=false` 的问题，支持知识库和时间范围过滤。
- 关键接口：`GET /api/admin/chat/missed-questions`。
- 关键表：`chat_record`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：需要准备一个未命中样例。
- 面试展示价值：可讲如何用未命中问题反哺知识库。

## 14. 管理端反馈运营模块

- 已完成功能：分页查看用户反馈，支持 `LIKE` / `DISLIKE`、知识库和时间范围过滤。
- 关键接口：`GET /api/admin/chat/feedback`。
- 关键表：`chat_feedback`、`chat_record`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：需要准备一条 LIKE 和一条 DISLIKE 数据。
- 面试展示价值：可讲用户反馈如何进入运营闭环。

## 15. 管理端热门问题 / 基础统计模块

- 已完成功能：热门问题 TopN、问答总数、命中数、未命中数、命中率、反馈数、点赞 / 点踩数。
- 关键接口：`GET /api/admin/chat/hot-questions`、`GET /api/admin/chat/stats`。
- 关键表：`chat_record`、`chat_feedback`。
- 当前完成度：基本完成。
- 投简历前是否必须补齐：需要验收 `startTime > endTime` 参数错误场景。
- 面试展示价值：可讲基础运营分析价值。

## 16. 工程化与安全模块

- 已完成功能：Spring Security、JWT、管理员权限服务、Flyway、配置属性、AI Debug 访问保护、API Key 环境变量读取。
- 关键接口：`GET /api/system/runtime-mode`、`POST /debug/ai/embedding`、`POST /debug/ai/llm`。
- 关键表：所有 Flyway 管理表及业务表。
- 当前完成度：部分完成。
- 投简历前是否必须补齐：必须验证空库迁移、关闭或限制 debug 接口、检查密钥泄露。
- 面试展示价值：可讲工程化意识和安全边界。

## 17. 文档与测试模块

- 已完成功能：已有需求 / 设计文档、RAG 评估样例、部分单元测试。
- 关键接口：不适用。
- 关键表：不适用。
- 当前完成度：部分完成。
- 投简历前是否必须补齐：必须修复 README / docs / SQL / 注释乱码，补齐启动和 Apifox 验收说明。
- 面试展示价值：可体现项目收尾能力和技术文档能力。
