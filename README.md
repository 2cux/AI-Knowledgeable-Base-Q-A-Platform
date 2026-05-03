# AI Knowledge Base QA Platform

AI 知识库问答平台后端 MVP。项目基于 Spring Boot 构建，围绕“知识库创建 -> 文档上传 -> 文档解析切片 -> Embedding -> 检索 -> LLM 生成答案 -> 会话/反馈/管理端追溯”实现一条可本地复现的 RAG 问答主链路。

## 项目背景

本项目用于模拟企业内部知识库问答场景：用户上传 txt / md 文档到自己的知识库，系统将文档切分为 chunk 并生成 embedding，提问时在知识库范围内检索相关 chunk，再调用 LLM 生成带引用来源的答案。第三周收尾阶段的目标是让项目达到简历展示、面试讲解和本地验收可复现的状态。

当前实现是 MVP，不是生产级高并发系统。向量检索采用 MySQL 存储 embedding JSON + Java 侧 cosine similarity 计算；尚未接入 Milvus、pgvector、Qdrant 等生产级向量数据库。Redis 仅用于管理端基础统计接口缓存；RabbitMQ / 微服务拆分不是当前已实现能力，简历或面试描述中不要夸大。

## 技术栈

- Java 17
- Spring Boot 3.5.13
- Spring Web / Validation
- Spring Security + JWT
- MyBatis-Plus
- MySQL 8.x
- Flyway
- Lombok
- springdoc-openapi
- LLM API
- Embedding API
- Redis 7 (only for `GET /api/admin/chat/stats` cache)
- Maven

## 核心功能

- 用户注册、登录、JWT 鉴权。
- 知识库创建、分页查询、详情查询。
- txt / md 文档上传、本地文件存储、文档列表与详情。
- 文档解析、切片、重处理、任务记录。
- 文档 chunk embedding 生成、状态查询、失败信息记录。
- RAG 检索问答：按知识库检索 chunk，生成答案，返回 citations。
- 会话列表、会话详情、用户问答记录。
- LIKE / DISLIKE 反馈。
- 管理端问答记录、未命中问题、反馈运营、热门问题、基础统计。
- AI 运行模式只读检查，不返回密钥。
- 本地 AI debug 接口，受 profile、开关和管理员权限限制。

## 系统主链路

1. `POST /auth/register` 注册用户。
2. `POST /auth/login` 登录并获取 JWT。
3. `POST /api/kb` 创建知识库。
4. `POST /api/documents/upload-file` 上传 txt / md 文件。
5. `POST /api/documents/{documentId}/process` 解析并切片。
6. `POST /api/documents/{documentId}/embed` 为 chunk 生成 embedding。
7. `POST /api/chat/ask` 在知识库内发起 RAG 问答。
8. `GET /api/chat/records/{id}` 查看问答详情和引用。
9. `POST /api/chat/records/{id}/feedback` 提交反馈。
10. `GET /api/admin/chat/*` 使用管理员账号查看运营与追溯数据。

## 本地启动步骤

### 1. 准备环境

- JDK 17
- Maven 3.9+
- MySQL 8.x
- 可用的 LLM / Embedding API Key

### 2. 创建 MySQL 空库

```sql
CREATE DATABASE aikb
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

也可以使用单独验收库，例如：

```sql
CREATE DATABASE aikb_day2_docs_check
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 3. 配置环境变量

Windows PowerShell 示例：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/aikb?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-db-password"
$env:JWT_SECRET="replace-with-a-long-random-secret-at-least-32-chars"
$env:JWT_EXPIRATION="86400000"
$env:JWT_ISSUER="aikb-backend"
$env:APP_LLM_API_KEY="your-llm-api-key"
$env:APP_EMBEDDING_API_KEY="your-embedding-api-key"
```

macOS / Linux 示例：

```bash
export DB_URL="jdbc:mysql://localhost:3306/aikb?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
export DB_USERNAME="root"
export DB_PASSWORD="your-db-password"
export JWT_SECRET="replace-with-a-long-random-secret-at-least-32-chars"
export JWT_EXPIRATION="86400000"
export JWT_ISSUER="aikb-backend"
export APP_LLM_API_KEY="your-llm-api-key"
export APP_EMBEDDING_API_KEY="your-embedding-api-key"
```

如果 LLM 和 Embedding 使用同一个供应商密钥，也可以只配置 `OPENAI_API_KEY`，项目会作为兼容兜底读取。推荐显式配置 `APP_LLM_API_KEY` 和 `APP_EMBEDDING_API_KEY`，便于两条链路后续拆分。

### 4. 编译和测试

```bash
mvn -q clean compile
mvn -q test
```

### 5. 启动应用

```bash
mvn spring-boot:run
```

默认端口：`8080`。

OpenAPI / Swagger UI：`http://localhost:8080/swagger-ui.html`

## Redis Admin Stats Cache

`GET /api/admin/chat/stats` uses Redis as an optional cache for the base, unfiltered admin statistics response. Redis is not required for RAG, `chat/ask`, `retrieval/search`, document `process`, or document `embed`.

Start Redis with Docker:

```bash
docker run -d --name aikb-redis -p 6379:6379 redis:7
```

Optional redis-cli verification:

```bash
docker exec -it aikb-redis redis-cli
```

Related environment variables:

```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
REDIS_TIMEOUT=3000ms
ADMIN_STATS_CACHE_TTL_MINUTES=5
```

The cache key is `aikb:admin:chat:stats`. The default TTL is 5 minutes. If Redis is unavailable or serialization fails, the endpoint logs a throttled warn and falls back to MySQL.

## Flyway 自动迁移说明

项目启用了 Flyway：

- migration 目录：`src/main/resources/db/migration`
- 默认随 Spring Boot 启动自动执行。
- 空库启动时会从 `V1__init.sql` 顺序执行到当前最新 migration。
- Day 1 空库验证中已确认可从空库执行 18 个 migration，最终版本为 `V18__add_chat_feedback_admin_query_indexes.sql`。

本地新建空库后无需手工建表，只需保证 `DB_URL` 指向目标空库并启动应用。

## 环境变量说明

| 变量 | 说明 | 本地默认值 / 建议 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring profile | 默认 `dev` |
| `DB_URL` | MySQL JDBC 地址 | dev 默认指向 `jdbc:mysql://localhost:3306/aikb...` |
| `DB_USERNAME` | MySQL 用户名 | dev 默认 `root` |
| `DB_PASSWORD` | MySQL 密码 | dev 默认 `123456` |
| `JWT_SECRET` | JWT 签名密钥 | dev 有本地默认值；生产必须配置强随机值 |
| `JWT_EXPIRATION` | JWT 过期时间，毫秒 | 默认 `86400000` |
| `JWT_ISSUER` | JWT issuer | dev 默认 `aikb-backend` |
| `APP_LLM_API_KEY` | LLM API Key | 推荐显式配置 |
| `APP_EMBEDDING_API_KEY` | Embedding API Key | 推荐显式配置 |
| `OPENAI_API_KEY` | 通用兼容 API Key | 可作为 LLM / Embedding 兜底 |
| `APP_LLM_BASE_URL` | LLM 完整接口地址 | 默认配置中为完整 messages 地址 |
| `APP_EMBEDDING_BASE_URL` | Embedding 完整接口地址 | 默认配置中为完整 embeddings 地址 |
| `APP_LLM_MODEL` | LLM 模型名 | 默认 `claude-opus-4-6` |
| `APP_EMBEDDING_MODEL` | Embedding 模型名 | 默认 `text-embedding-3-large` |
| `APP_FILE_UPLOAD_DIR` | 本地上传目录 | 默认 `uploads` |
| `APP_RAG_RETRIEVAL_TOP_K` | 默认检索 TopK | 默认 `5` |
| `APP_RAG_RETRIEVAL_MIN_EFFECTIVE_SCORE` | 有效命中分数阈值 | 默认 `0.2` |

`dev` 中的数据库密码、JWT secret 仅用于本地开发，生产环境必须通过环境变量覆盖，不应使用仓库默认值。

## LLM / Embedding API Key 配置说明

项目不会在仓库中保存真实 API Key，也不应在日志或响应中输出密钥。

优先级：

1. LLM 读取 `APP_LLM_API_KEY`，兜底读取 `OPENAI_API_KEY`。
2. Embedding 读取 `APP_EMBEDDING_API_KEY`，兜底读取 `OPENAI_API_KEY`。
3. LLM base url 读取 `APP_LLM_BASE_URL` 或 `OPENAI_LLM_BASE_URL`。
4. Embedding base url 读取 `APP_EMBEDDING_BASE_URL` 或 `OPENAI_EMBEDDING_BASE_URL`。

注意：当前 `base-url` 需要填写完整接口地址，不是仅填写 `/v1` 根路径。

缺少 Key 时，相关接口会返回业务错误，例如：

- LLM：`LLM api-key 未配置，请通过环境变量 APP_LLM_API_KEY 或 OPENAI_API_KEY 注入`
- Embedding：`embedding api-key 未配置，请通过环境变量 APP_EMBEDDING_API_KEY 或 OPENAI_API_KEY 注入`

## dev / prod Profile 区别

### dev

- 默认 profile：`dev`。
- 数据库连接有本地默认值，便于 fresh clone 后启动。
- JWT secret 有本地开发默认值，只能用于本地。
- `/debug/ai/**` 在 dev profile 下可被守卫判定为启用，但仍需要登录且当前用户为管理员。

### prod

- 数据库连接必须通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 注入。
- `JWT_SECRET` 必须显式配置。
- `/debug/ai/**` 强制拒绝访问，即使配置开关被打开也不应放行。
- 不应使用任何 dev 默认密码或默认 JWT secret。

## debug 接口安全说明

调试接口路径：

- `POST /debug/ai/embedding`
- `POST /debug/ai/llm`

这些接口只用于本地联调外部 AI 能力。访问规则由 `AiDebugAccessGuard` 收口：

- prod profile 下强制拒绝。
- dev profile 下允许进入调试守卫，但仍要求管理员权限。
- 非 dev 环境需要显式开启 `app.debug.ai-test.enabled`，且仍要求管理员权限。
- debug 接口仅用于本地联调，可返回上游调试响应内容，但不会返回真实 API Key。

## 核心接口调用顺序

建议按以下顺序在 Apifox 验收：

1. 注册：`POST /auth/register`
2. 登录：`POST /auth/login`
3. 创建知识库：`POST /api/kb`
4. 上传文件：`POST /api/documents/upload-file`
5. 查询文档状态：`GET /api/documents/{documentId}/status`
6. 解析切片：`POST /api/documents/{documentId}/process`
7. 查询 chunk：`GET /api/documents/{documentId}/chunks`
8. 执行 embedding：`POST /api/documents/{documentId}/embed`
9. 查询 embedding 状态：`GET /api/documents/{documentId}/embedding-status`
10. 可选只检索：`POST /api/retrieval/search`
11. RAG 提问：`POST /api/chat/ask`
12. 问答详情：`GET /api/chat/records/{id}`
13. 会话列表：`GET /api/chat/conversations`
14. 会话详情：`GET /api/chat/conversations/{conversationId}`
15. 提交反馈：`POST /api/chat/records/{id}/feedback`
16. 管理端追溯：`GET /api/admin/chat/records/{id}`
17. 管理端未命中、反馈、热门问题、统计接口。

除注册、登录外，请在请求头中加入：

```text
Authorization: Bearer <token>
```

## Apifox 验收建议

详细验收用例见：

- `docs/week3/acceptance-test-plan.md`
- `docs/week3/api-overview.md`

建议准备两个账号：

- 普通用户：用于注册、登录、创建知识库、上传文档、提问、反馈。
- 管理员用户：用于访问 `/api/admin/chat/**` 和 `/api/system/runtime-mode`。

注册接口默认创建普通用户。管理员账号可在本地验收库中手工将 `user.role` 改为 `ADMIN`，或使用已有管理员数据。验收时所有 ID 都应使用前置接口返回的真实值，不要写死示例 ID。

建议重点覆盖：

- 正常链路：上传 -> process -> embed -> ask -> feedback -> admin query。
- 权限链路：未登录、访问他人资源、普通用户访问管理端。
- 失败链路：缺少 API Key、文档未处理、文档未 embedding、知识库外问题导致未命中。
- 生命周期链路：`force=true` 重新 process / embed、删除文档后关联数据清理。

## 当前 MVP 边界

- 当前向量检索是 MySQL 存储 `chunk_embedding.vector_json` + Java cosine similarity 的 MVP 实现。
- 当前没有接入生产级向量数据库。
- 当前仅为 `GET /api/admin/chat/stats` 实现 Redis 可降级缓存，未把 Redis 接入 RAG、文档处理或消息队列链路。
- 当前没有接入 RabbitMQ、Kafka 或异步消息队列。
- 当前不是微服务架构，也未实现分布式任务调度。
- 当前文件存储为本地目录，不是对象存储。
- 当前文档解析只覆盖 txt / md 等轻量文本类文件，不是完整 Office / PDF 解析平台。
- 当前管理端是后端接口能力，没有独立前端管理台。
- 当前权限模型为用户资源归属 + 管理员角色，未实现复杂 RBAC。

## 后续优化方向

- 接入 pgvector、Milvus、Qdrant 等向量检索能力，替换 Java 侧全量候选 cosine 计算。
- 增加混合检索、rerank、查询改写和可配置提示词模板。
- 引入更完整的文档解析能力，例如 PDF、Word、HTML。
- 增加异步任务队列、任务重试、进度推送和失败补偿。
- 增加对象存储、文件病毒扫描、内容安全审计。
- 完善管理员后台 UI、知识库运营看板和 RAG 评估集。
- 增加生产监控、限流、审计日志和更细粒度权限。
