# AI Knowledge Base QA Platform

## 本地 AI 配置说明

本项目的 LLM 和 Embedding 敏感配置必须通过环境变量注入，仓库中不保留任何真实 API Key。

### API Key

项目优先读取以下环境变量：

- `APP_LLM_API_KEY`
- `APP_EMBEDDING_API_KEY`

如果两条链路共用同一个供应商密钥，也兼容读取：

- `OPENAI_API_KEY`

Windows PowerShell：

```powershell
$env:OPENAI_API_KEY="your-real-api-key"
$env:APP_LLM_API_KEY=$env:OPENAI_API_KEY
$env:APP_EMBEDDING_API_KEY=$env:OPENAI_API_KEY
```

Windows 长期环境变量可使用 `setx`，但只会对新打开的终端和新启动的应用进程生效：
```powershell
setx OPENAI_API_KEY "your-real-api-key"
setx APP_LLM_API_KEY "your-real-api-key"
setx APP_EMBEDDING_API_KEY "your-real-api-key"
```

macOS / Linux：

```bash
export OPENAI_API_KEY="your-real-api-key"
export APP_LLM_API_KEY="$OPENAI_API_KEY"
export APP_EMBEDDING_API_KEY="$OPENAI_API_KEY"
```

### Base URL

如果聊天和 embedding 走不同网关，优先分别配置：

- `APP_LLM_BASE_URL`
- `APP_EMBEDDING_BASE_URL`

项目也兼容以下别名：

- `OPENAI_LLM_BASE_URL`
- `OPENAI_EMBEDDING_BASE_URL`

为避免把通用根地址误当成完整接口地址，当前不再直接读取 `OPENAI_BASE_URL`。

注意：本项目当前 `base-url` 需要填写“完整接口地址”，不是仅 `/v1` 根路径。

Windows PowerShell：

```powershell
$env:APP_LLM_BASE_URL="https://your-gateway.example.com/v1/messages"
$env:APP_EMBEDDING_BASE_URL="https://your-gateway.example.com/v1/embeddings"
```

macOS / Linux：

```bash
export APP_LLM_BASE_URL="https://your-gateway.example.com/v1/messages"
export APP_EMBEDDING_BASE_URL="https://your-gateway.example.com/v1/embeddings"
```

### 模型配置

可通过以下环境变量覆盖默认模型：

- `APP_LLM_MODEL`
- `APP_EMBEDDING_MODEL`

示例：

```powershell
$env:APP_LLM_MODEL="claude-opus-4-6"
$env:APP_EMBEDDING_MODEL="text-embedding-3-large"
```

```bash
export APP_LLM_MODEL="claude-opus-4-6"
export APP_EMBEDDING_MODEL="text-embedding-3-large"
```

### 缺少 API Key 时的表现

- `chat ask`、`/debug/ai/llm` 等依赖 LLM 的接口会返回清晰业务错误：
  `LLM api-key 未配置，请通过环境变量 APP_LLM_API_KEY 或 OPENAI_API_KEY 注入`
- `embedding`、RAG 检索等依赖向量化的链路会返回清晰业务错误：
  `embedding api-key 未配置，请通过环境变量 APP_EMBEDDING_API_KEY 或 OPENAI_API_KEY 注入`
- 系统不会在响应或日志中输出真实 API Key，也不会打印完整 `Authorization` Header。

### 启动示例

```powershell
mvn spring-boot:run
```

```bash
mvn spring-boot:run
```
