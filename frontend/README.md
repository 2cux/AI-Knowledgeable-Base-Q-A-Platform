# Frontend

AI 知识库问答平台前端项目，使用 React + Vite + TypeScript + React Router + Axios + Tailwind CSS。

## 开发

```bash
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

## 构建

```bash
npm run build
```

## 环境变量

开发环境后端地址配置在 `.env.development`：

```text
VITE_API_BASE_URL=http://localhost:8080
```

可以从 `.env.example` 复制一份本地配置。修改环境变量后需要重启 Vite dev server 才会生效。

## 目录

```text
src/
├── api/
├── components/
├── pages/
├── router/
├── types/
└── utils/
```
