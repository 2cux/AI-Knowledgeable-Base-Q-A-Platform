# AI 知识库问答平台

## 项目简介

AI 知识库问答平台是一个基于 Spring Boot + RAG 的企业知识库问答系统后端原型。项目围绕“知识统一接入、文档解析切片、embedding 向量化、RAG 检索、LLM 生成回答”构建，当前支持上传 `txt` / `md` 文档，并基于知识库内容进行检索问答。

系统已实现文档解析、切片、embedding 生成、知识库内检索、答案来源引用、统一拒答、多轮会话、问答日志、用户反馈、日志脱敏和敏感输出控制等能力。它适合作为企业内部知识问答、客服知识库、新员工培训问答等场景的后端 MVP 原型。

当前项目核心功能已经开发完成，并通过 Apifox 完整主链路验收。

## 项目背景

企业内部知识通常分散在文档、制度、FAQ、培训材料和历史问答中，员工查找成本高，客服或运营人员也容易出现答复口径不一致的问题。传统关键词检索只能返回文档列表，仍需要人工阅读和筛选；直接让大模型回答又可能出现无依据生成。

本项目采用 RAG 方式：先从指定知识库中检索相关文档切片，再将检索结果作为上下文交给 LLM 生成回答。这样可以让回答尽量基于知识库内容生成，并在无有效命中时执行统一拒答策略，降低大模型“凭空回答”的风险。

## 核心功能

### 1. 用户与权限

- 用户注册
- 用户登录
- 当前用户信息查询
- JWT 鉴权
- 管理员接口权限保护

### 2. 知识库管理

- 创建知识库
- 分页查询知识库
- 查询知识库详情
- 更新知识库信息
- 逻辑删除知识库
- 删除后业务拦截：已删除知识库不能继续上传文档、执行 process、执行 embed 或发起 chat/ask

### 3. 文档处理

- 上传 `txt` / `md` 文件
- 查询文档列表、详情和状态
- 文档解析
- 文档切片
- 查询文档 chunk
- 重复处理时清理旧 chunk 和旧 embedding，保证数据一致性

### 4. Embedding

- 文本向量生成
- embedding 模型配置化
- `vectorSize` 配置化
- 向量维度校验
- embedding 状态查询和失败信息记录

### 5. RAG 问答

- 基于知识库检索相关 chunk
- 构建 RAG prompt
- 调用 LLM 生成回答
- 返回答案引用来源
- 无有效命中时统一拒答
- 支持多轮追问和会话上下文

### 6. 日志与运营

- 问答日志
- 问答详情查询
- 用户 LIKE / DISLIKE 反馈
- 未命中问题收集
- 管理端查看问答记录、未命中问题、反馈、热门问题和基础统计
- RAG / LLM 调用日志脱敏

### 7. 安全控制

- 敏感日志脱敏
- 敏感输出控制
- debug 接口默认关闭或仅在受控条件下开放
- 管理端接口权限校验
- CORS 最小化配置

## 技术栈

- Java 17
- Spring Boot 3.5.13
- Spring Web
- Spring Security + JWT
- Spring Validation
- MyBatis-Plus
- MySQL 8.x
- Flyway
- OpenAI / 兼容 LLM API
- Embedding API
- Redis：用于管理端统计和热门问题缓存，可降级回 MySQL 查询
- RabbitMQ：用于文档解析 / 切片和 embedding 生成异步任务
- springdoc-openapi
- Apifox
- Maven

说明：当前向量检索采用 MySQL 存储 embedding JSON，并在 Java 侧计算相似度；项目尚未接入独立向量数据库。

## 系统架构 / 核心链路

主链路如下：

```text
用户登录
-> 创建知识库
-> 上传 txt / md 文档
-> 文档解析
-> 文档切片
-> 生成 embedding
-> 用户提问
-> RAG 检索相关 chunk
-> 构建 prompt
-> 调用 LLM
-> 返回答案与引用来源
-> 记录问答日志
```

从数据流看，文档先被解析成文本切片，切片生成 embedding 后落库；用户提问时，系统为问题生成 query embedding，在指定知识库范围内检索相似 chunk，再将命中的上下文交给 LLM 生成最终回答。如果没有达到有效命中阈值，系统不会调用 LLM 编造答案，而是返回统一拒答结果并记录未命中问题。

## 快速启动

### 1. 克隆项目

```bash
git clone <your-repository-url>
cd <your-project-directory>
```

### 2. 创建 MySQL 数据库

```sql
CREATE DATABASE aikb
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 3. 执行 SQL / Flyway 迁移

项目使用 Flyway 管理数据库迁移，迁移脚本位于：

```text
src/main/resources/db/migration
```

本地新建空库后，启动应用时会自动执行迁移。当前已包含 `V1__init.sql` 至 `V19__add_knowledge_base_deleted.sql`。

### 4. 配置环境变量

建议复制示例文件后在本地填写真实配置，避免把 API Key、数据库密码、JWT 密钥写入仓库：

```bash
cp .env.example .env
```

Windows PowerShell 也可以手动复制：

```powershell
Copy-Item .env.example .env
```

根据本地环境至少修改 `.env` 中的 MySQL、RabbitMQ、JWT、LLM 和 Embedding 配置。`APP_EMBEDDING_VECTOR_SIZE` 必须与实际 embedding 模型返回维度一致，否则生成或检索 embedding 时会失败。

如果 LLM 和 embedding 使用同一个兼容 OpenAI 协议的服务，可以使用 `OPENAI_API_KEY` 作为兜底密钥；更推荐分别配置 `APP_LLM_API_KEY` 和 `APP_EMBEDDING_API_KEY`。

`APP_DEBUG_API_ENABLED` 默认必须保持 `false`。本地开发如需调试 `/debug/**` 接口，需要显式设置 `APP_DEBUG_API_ENABLED=true`，并使用管理员账号访问。

完整执行文档 process / embed 主链路时，需要保证 RabbitMQ 可访问。Redis 主要用于管理端统计和热门问题缓存，不可用时相关查询会降级回 MySQL。

不要将 `.env`、真实 `application-local.yml`、真实 `application-secret.yml`、真实 API Key 或数据库密码提交到 Git。

### 5. 编译和启动项目

```bash
mvn -q -DskipTests compile
mvn spring-boot:run
```

默认端口：

```text
http://localhost:8080
```

Swagger UI：

```text
http://localhost:8080/swagger-ui.html
```

### 6. 访问接口

除注册、登录外，业务接口需要在请求头中携带：

```text
Authorization: Bearer <token>
```

## 环境变量配置

项目提供 `.env.example` 和 `src/main/resources/application-example.yml` 作为配置模板。真实运行时优先通过环境变量覆盖配置；如果需要维护本地 YAML，请使用 `application-local.yml` 或 `application-secret.yml`，并确保它们不会提交到 Git。

### 必填配置

| 环境变量 | 对应配置 | 说明 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `spring.datasource.url` | MySQL 地址组件；也可以直接使用 `DB_URL` 覆盖完整 JDBC URL |
| `DB_USERNAME` | `spring.datasource.username` | MySQL 用户名 |
| `DB_PASSWORD` | `spring.datasource.password` | MySQL 密码 |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `spring.rabbitmq.host` / `spring.rabbitmq.port` | 文档处理和 embedding 异步任务依赖 RabbitMQ |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `spring.rabbitmq.username` / `spring.rabbitmq.password` | RabbitMQ 账号 |
| `JWT_SECRET` | `jwt.secret` | JWT 签名密钥，生产环境必须使用强随机值 |
| `APP_LLM_BASE_URL` | `app.llm.base-url` | LLM 接口完整地址 |
| `APP_LLM_API_KEY` | `app.llm.api-key` | LLM API Key |
| `APP_LLM_MODEL` | `app.llm.model` | LLM 模型名称 |
| `APP_EMBEDDING_BASE_URL` | `app.embedding.base-url` | Embedding 接口完整地址 |
| `APP_EMBEDDING_API_KEY` | `app.embedding.api-key` | Embedding API Key |
| `APP_EMBEDDING_MODEL` | `app.embedding.model` | Embedding 模型名称 |
| `APP_EMBEDDING_VECTOR_SIZE` | `app.embedding.vector-size` | Embedding 向量维度，必须与模型实际返回维度一致 |

### 可选配置

| 环境变量 | 对应配置 | 说明 |
|---|---|---|
| `DB_URL` | `spring.datasource.url` | 完整 MySQL JDBC URL；配置后优先于 `DB_HOST` / `DB_PORT` / `DB_NAME` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `spring.data.redis.*` | 管理端统计和热门问题缓存；当前不作为主链路强依赖 |
| `JWT_EXPIRATION` / `JWT_ISSUER` | `jwt.expiration` / `jwt.issuer` | Token 过期时间和签发方 |
| `APP_FILE_UPLOAD_DIR` | `app.file.upload-dir` | 文件上传目录，默认 `uploads` |
| `APP_FILE_MAX_SIZE` | `app.file.max-size` 和 multipart 限制 | 单文件上传大小，默认 `20MB` |
| `APP_DEBUG_API_ENABLED` | `app.debug-api.enabled` | debug 接口开关，默认 `false` |
| `APP_CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` | 逗号分隔的允许跨域来源 |
| `APP_RAG_RETRIEVAL_TOP_K` | `app.rag.retrieval.top-k` | RAG 检索 TopK，默认 `5` |
| `APP_RAG_RETRIEVAL_MIN_EFFECTIVE_SCORE` | `app.rag.retrieval.min-effective-score` | 有效命中分数阈值，默认 `0.2` |
| `OPENAI_API_KEY` | `app.llm.api-key` / `app.embedding.api-key` | LLM 和 Embedding 共用供应商时的兜底密钥 |

本地 `dev` 环境默认允许以下 CORS 来源，也可以通过 `APP_CORS_ALLOWED_ORIGINS` 覆盖：

```text
http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173
```

配置校验建议：

1. `APP_LLM_API_KEY` 为空时，调用问答接口应返回明确错误。
2. `APP_EMBEDDING_API_KEY` 为空时，调用 embedding 接口应返回明确错误。
3. `APP_EMBEDDING_VECTOR_SIZE` 应与模型返回向量维度一致。
4. `APP_DEBUG_API_ENABLED` 默认保持 `false`。
5. 文件上传目录不存在时，系统应自动创建或返回明确错误。

## 接口调用顺序

建议按以下顺序在 Apifox 中执行主链路验收：

1. 注册：`POST /auth/register`
2. 登录：`POST /auth/login`
3. 获取当前用户：`GET /auth/me`
4. 创建知识库：`POST /api/kb`
5. 查询知识库：`GET /api/kb`、`GET /api/kb/{id}`
6. 上传 `txt` / `md` 文件：`POST /api/documents/upload-file`
7. 查询文档状态：`GET /api/documents/{documentId}/status`
8. 解析 / 切片：`POST /api/documents/{documentId}/process`
9. 查询文档切片：`GET /api/documents/{documentId}/chunks`
10. 生成 embedding：`POST /api/documents/{documentId}/embed`
11. 查询 embedding 状态：`GET /api/documents/{documentId}/embedding-status`
12. 可选检索验证：`POST /api/retrieval/search`
13. 发起 RAG 问答：`POST /api/chat/ask`
14. 多轮追问：继续调用 `POST /api/chat/ask`，并携带同一 `conversationId`
15. 查询会话：`GET /api/chat/conversations`、`GET /api/chat/conversations/{conversationId}`
16. 查询问答记录：`GET /api/chat/records`、`GET /api/chat/records/{id}`
17. 提交反馈：`POST /api/chat/records/{id}/feedback`
18. 管理端查询：`GET /api/admin/chat/records`、`GET /api/admin/chat/missed-questions`、`GET /api/admin/chat/feedback`、`GET /api/admin/chat/hot-questions`、`GET /api/admin/chat/stats`
19. 删除知识库：`DELETE /api/kb/{id}`
20. 删除知识库后的拦截验证：再次尝试上传、process、embed、chat/ask，预期被业务拦截

debug 接口仅用于受控联调：

```text
POST /debug/ai/embedding
POST /debug/ai/llm
```

## Apifox 验收说明

项目已通过 Apifox 完整主链路测试。验收重点覆盖：

- 注册 / 登录
- 当前用户信息查询
- 知识库创建、查询、更新、逻辑删除
- `txt` / `md` 上传
- 非法文件拒绝
- 文档 process
- embedding 生成
- RAG 命中问答
- RAG 无命中拒答
- 答案引用来源
- 多轮追问
- 问答日志
- 用户反馈
- 未命中问题收集
- 管理员接口权限保护
- 敏感输出控制
- debug 接口关闭验证
- 删除知识库后的业务拦截

## 当前限制

1. 当前只支持 `txt` / `md` 文件。
2. 暂未支持 PDF / Word 解析。
3. 当前是后端项目，没有完整前端页面。
4. 当前权限控制属于 MVP 级别，主要包括登录用户资源归属校验和管理员接口校验，不是复杂企业级 RBAC。
5. 当前知识库删除采用逻辑删除，关联文档、chunk、embedding、日志会保留。
6. 当前向量检索仍基于 MySQL 存储向量 JSON 和 Java 侧相似度计算，尚未接入独立向量数据库。
7. 当前多来源接入、复杂审核流、BI 报表等属于后续规划。

## 后续规划

- 支持 PDF / Word 解析
- 接入向量数据库，例如 pgvector、Milvus 或 Qdrant
- 增加前端管理页面
- 增加知识库质量评分
- 增加多知识库路由
- 增加更细粒度权限控制
- 增加管理端数据看板
- 接入企业内部系统或工单系统
- 优化检索策略，例如混合检索、rerank、query rewrite

## 项目亮点

1. 实现从文档上传、解析切片、embedding 到 RAG 问答的完整闭环。
2. 通过统一拒答策略降低大模型无依据回答风险。
3. 支持答案引用来源，提高回答可追溯性。
4. 支持多轮会话，让追问可以复用上下文。
5. 对 LLM / RAG 日志进行脱敏，降低敏感信息泄露风险。
6. 通过 debug 接口安全收口和管理员权限校验增强系统安全性。
7. 支持知识库逻辑删除后的业务拦截，避免删除资源继续参与上传、处理、向量化和问答。
8. 使用 Apifox 完成完整主链路验收，覆盖正常流、异常流和安全流。
