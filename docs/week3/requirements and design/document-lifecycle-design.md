# 第三周文档生命周期设计

## 1. 设计目标

在不推翻当前文档主链路的前提下，补齐文档生命周期最小治理能力，重点解决：

- 文档删除。
- 文档重处理。
- 文档重新 embedding。
- 状态可见性。
- 替换更新与版本基础语义。

## 2. 当前实现基线

当前文档相关核心对象：

- `document`
- `document_chunk`
- `chunk_embedding`
- `task_record`

当前主链路已经存在，但生命周期存在明显不足：

- `process` 对已 `CHUNKED` 文档直接拦截。
- 删除能力缺失。
- embedding 可重跑但缺少正式业务语义。
- 状态汇总虽有基础接口，但对外可见性不足。

## 3. 设计原则

- 以现有数据模型为核心。
- 尽量避免大表重构。
- 先保证一致性，再做体验增强。
- 所有生命周期动作都必须考虑 chunk / embedding / retrieval 连带影响。

## 4. 状态机设计

## 4.1 文档状态机建议

当前文档至少已经存在：

- `UPLOADED`
- `PENDING`
- `CHUNKING`
- `CHUNKED`

第三周建议补齐并规范：

- `UPLOADED`
- `PENDING`
- `CHUNKING`
- `CHUNKED`
- `FAILED`
- `DELETED` 或逻辑删除态

### 状态语义

- `UPLOADED`：文件已保存，尚未进入解析。
- `PENDING`：已创建处理任务，等待执行。
- `CHUNKING`：正在解析/切片。
- `CHUNKED`：切片完成。
- `FAILED`：最近一次处理失败。
- `DELETED`：文档已删除，不再参与检索。

## 4.2 embedding 汇总状态

文档级 embedding 状态建议继续由 `chunk_embedding` 聚合得出：

- `NOT_CHUNKED`
- `PENDING`
- `PROCESSING`
- `SUCCESS`
- `FAILED`
- `PARTIAL`

第三周不强制新增独立文档 embedding 状态表。

## 5. 删除设计

## 5.1 删除策略建议

第三周建议采用：

- 业务接口层表现为“删除文档”。
- 数据层优先采用“逻辑删除 or 物理删除 + 级联清理”的明确策略。

### 推荐方案

如果当前 `document` 表没有删除字段，第三周可先采用**受控物理删除**，但必须确保：

- 先校验 owner 权限。
- 先处理 chunk 与 embedding 清理。
- 再删除 document 记录。
- 最后删除本地文件。

### 不建议

- 只删 document，不删 chunk 和 embedding。
- 只改状态，不处理检索脏数据。

## 5.2 删除联动关系

```text
delete document
  -> delete chunk_embedding by document_id
  -> delete document_chunk by document_id
  -> delete document
  -> delete local file
```

如果后续版本改成逻辑删除，则检索查询必须同步过滤删除态。

## 6. 重新处理设计

## 6.1 现状问题

当前 `process` 对已 `CHUNKED` 文档会直接拒绝。

## 6.2 第三周建议

提供显式重处理语义，推荐两种方案二选一：

### 方案 A

- 保留 `POST /api/documents/{id}/process`
- 在请求体中增加 `force`

### 方案 B

- 新增 `POST /api/documents/{id}/reprocess`

### 推荐结论

第三周推荐方案 A：

- 改动最小。
- 与现有 `DocumentProcessRequest` 兼容演进更顺滑。

## 6.3 重处理联动逻辑

```text
force reprocess
  -> 校验文档归属
  -> 创建 task_record
  -> 将文档置为 CHUNKING
  -> 清理旧 chunk_embedding
  -> 清理旧 document_chunk
  -> 重新解析文件
  -> 重新生成 chunk
  -> 更新 parse_status
```

### 注意

必须先清旧 embedding，再清旧 chunk，避免孤儿向量状态。

## 7. 重新 embedding 设计

## 7.1 目标

让“重新 embedding”成为正式产品动作，而不只是技术上可重复调用现有接口。

## 7.2 建议方案

保留现有：

- `POST /api/documents/{id}/embed`

在请求中支持：

- 指定 `embeddingModel`
- 增加 `force`

### force 语义

- 非 force：只处理未成功的 chunk。
- force：清理旧成功/失败状态，按当前模型重建。

### 第三周最小实现建议

如果实现复杂度受限，第三周可统一采用“全量重建当前文档 embedding 状态”的方式，但必须文档化说明。

## 8. 状态可见性设计

## 8.1 统一状态接口建议

当前已有 `/status` 与 `/embedding-status`。

第三周建议增强但不重复造接口：

- `GET /api/documents/{id}/status`
  - 聚合文档解析、切片、embedding、最近任务、最近错误。

- `GET /api/documents/{id}/embedding-status`
  - 保留 chunk 级明细。

### 建议状态接口字段

- `documentId`
- `knowledgeBaseId`
- `parseStatus`
- `chunkCount`
- `embeddingStatus`
- `embeddingSuccessCount`
- `embeddingFailedCount`
- `latestTaskType`
- `latestTaskStatus`
- `latestErrorMessage`
- `canReprocess`
- `canReembed`

## 9. 替换更新设计

## 9.1 最小实现目标

第三周不做复杂版本平台，只做“基础替换”能力。

### 建议方案

- 新增替换上传接口，或在原上传接口上支持“替换现有文档”。
- 替换后：
- 老版本置为失效。
- 新版本成为当前有效版本。
- 新版本重新走解析、切片、embedding 链路。

## 9.2 数据模型影响

第三周如要做最小版本语义，建议在 `document` 表补充以下字段中的一部分：

- `source_document_id`
- `version_no`
- `is_current`
- `status`

### 最小可选方案

如果第三周不改太多表结构，也可以先只加：

- `source_document_id`
- `is_current`

这样即可支撑“某文档被替换过”与“当前有效版本”的基本语义。

## 10. 检索联动设计

生命周期能力必须保证检索侧一致性。

### 基本规则

- 已删除文档的 chunk 不能继续被检索。
- 已替换旧版本文档的 chunk 不能继续被检索。
- 重新处理后旧 chunk 不应残留。
- 重新 embedding 后旧向量状态不应继续参与检索。

### 对当前架构的要求

由于当前检索直接读取 `chunk_embedding + document_chunk + document`，因此第三周必须保证生命周期动作同步清理这些基础表，否则检索会立即出脏数据。

## 11. 兼容现有实现策略

- 保留现有上传、处理、embedding 主路径。
- 对旧文档数据不做破坏性迁移。
- 新增字段优先向后兼容。
- 当前 txt/md 主链路必须继续稳定，不因生命周期补齐而退化。

## 12. 风险与注意事项

- 生命周期改造最容易引发数据一致性问题。
- 删除、替换、重建都需要明确事务边界。
- 本地文件删除失败、数据库删除失败、状态更新失败都必须有兜底和日志。

## 13. 第三周完成标准

- 文档删除可安全执行。
- 重处理和重新 embedding 有正式语义和接口支持。
- 文档状态视图能真实反映当前生命周期状态。
- 替换更新若进入第三周实现，应至少具备最小版本语义，不产生旧内容继续参与检索的问题。
