# AI Knowledge Base QA Platform

AI 知识库问答平台采用前后端分离 monorepo 结构：

```text
AI-knowledge-base-QA-platform/
├── backend/      # Spring Boot 后端
├── frontend/     # React + Vite 前端
├── docs/         # 项目文档
├── README.md     # 总项目说明
└── .gitignore
```

## 子项目

- 后端：Spring Boot 3.5.x，源码与 Maven 配置位于 `backend/`。
- 前端：React + Vite + TypeScript，源码与 npm 配置位于 `frontend/`。
- 文档：需求、设计、测试与阶段总结保留在 `docs/`。

## 本地启动

### 后端

```bash
cd backend
mvn -q -DskipTests compile
mvn spring-boot:run
```

默认地址：

```text
http://localhost:8080
```

Flyway 脚本位于：

```text
backend/src/main/resources/db/migration
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：

```text
http://localhost:5173
```

前端开发环境通过 `frontend/.env.development` 读取后端地址：

```text
VITE_API_BASE_URL=http://localhost:8080
```

## 联调要点

1. 先启动后端，确认 `http://localhost:8080/swagger-ui.html` 可访问。
2. 再启动前端，访问 `http://localhost:5173`。
3. 未登录访问 `/kb`、`/chat`、`/admin` 等受保护路由会跳转到 `/login`。
4. 前端 Axios 默认读取 `VITE_API_BASE_URL`，并自动携带本地 token。
5. 后端 dev 配置已允许 `http://localhost:5173` 和 `http://127.0.0.1:5173` 跨域访问。

更多后端说明见 `backend/README.md`，前端说明见 `frontend/README.md`。
