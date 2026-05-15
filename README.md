# AI Knowledge Base QA Platform

AI 知识库问答平台，支持文档上传、RAG 检索增强生成、多轮对话，采用前后端分离 monorepo 结构。

```text
AI-knowledge-base-QA-platform/
├── backend/      # Spring Boot 3.5.x 后端
├── frontend/     # React + Vite + TypeScript 前端
├── docs/         # 项目文档
├── docker-compose.yml  # MySQL / Redis / RabbitMQ
├── .env.example        # 环境变量模板
├── start-backend.bat   # Windows 后端启动脚本
├── start-frontend.bat  # Windows 前端启动脚本
├── start-backend.sh    # macOS/Linux 后端启动脚本
├── start-frontend.sh   # macOS/Linux 前端启动脚本
└── README.md
```

## Quick Start

### 前置要求

- **Java 21+**
- **Node.js 18+ & npm**
- **Docker & Docker Compose**

### 1. 克隆项目

```bash
git clone <repo-url>
cd AI-knowledge-base-QA-platform
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

> 编辑 `.env`，填入 LLM 和 Embedding 的 API Key 等信息。

### 3. 启动中间件 (Docker)

```bash
docker compose up -d
```

将启动 MySQL 8 (`:3306`)、Redis 7 (`:6379`)、RabbitMQ 3 (`:5672`，管理后台 `:15672`)。

### 4. 启动后端

```bash
# Windows
start-backend.bat

# macOS / Linux
./start-backend.sh
```

后端启动后会自动执行 Flyway 数据库迁移，默认地址：`http://localhost:8080`

Swagger 文档：`http://localhost:8080/swagger-ui.html`

### 5. 启动前端

```bash
# Windows
start-frontend.bat

# macOS / Linux
./start-frontend.sh
```

前端默认地址：`http://localhost:5173`

### 完成

浏览器访问 `http://localhost:5173` 即可使用。

## 项目结构

### 后端

- **框架**: Spring Boot 3.5.x, MyBatis-Plus, Flyway
- **构建**: Maven
- **入口**: `backend/`

### 前端

- **框架**: React 19, Vite 8, TypeScript, Tailwind CSS
- **构建**: npm
- **入口**: `frontend/`

## 联调要点

> 更多后端说明见 `backend/README.md`，前端说明见 `frontend/README.md`。

1. 先启动后端，确认 `http://localhost:8080/swagger-ui.html` 可访问。
2. 再启动前端，访问 `http://localhost:5173`。
3. 未登录访问 `/kb`、`/chat`、`/admin` 等受保护路由会跳转到 `/login`。
4. 前端 Axios 默认读取 `VITE_API_BASE_URL`，并自动携带本地 token。
5. 后端 dev 配置已允许 `http://localhost:5173` 和 `http://127.0.0.1:5173` 跨域访问。
