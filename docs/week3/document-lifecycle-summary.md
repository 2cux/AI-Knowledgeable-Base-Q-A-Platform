# 文档生命周期总结

## 1. 文档上传后的状态流转

当前文档从上传到可问答，大致经历以下状态：

1. 上传文档后写入 `document`，`parse_status=NOT_STARTED`，`embedding_status=NOT_STARTED`。
2. 执行 `process` 后解析文本并生成 chunk，成功后 `parse_status=SUCCESS`。
3. 执行 `embed` 后为 chunk 生成向量，成功后 `embedding_status=SUCCESS`。
4. 问答检索时只使用成功写入 `chunk_embedding` 的向量。

相关状态字段包括：

- `parse_status`
- `embedding_status`
- `chunk_count`
- `embedded_chunk_count`
- `latest_task_type`
- `latest_task_status`
- `latest_error_message`

## 2. process 的作用

`POST /api/documents/{documentId}/process` 负责把文档内容变成可检索的文本切片。

它会：

- 校验文档属于当前用户。
- 防止重复处理正在执行的文档。
- 解析真实文件或请求中的 `textContent`。
- 按 `chunkSize` 和 `overlap` 切片。
- 清理旧 chunk 和旧 embedding。
- 写入新的 `document_chunk`。
- 更新文档状态和任务状态。

## 3. embed 的作用

`POST /api/documents/{documentId}/embed` 负责把 `document_chunk` 转成向量。

它会：

- 校验文档已成功切片。
- 遍历可向量化的 chunk。
- 调用 Embedding API 或本地 / Mock 实现，需以配置为准。
- 将向量 JSON 写入 `chunk_embedding.vector_json`。
- 记录每个 chunk 的状态、错误信息和模型名。
- 聚合更新文档级 embedding 状态。

## 4. 文档状态字段

当前文档状态不是单一字段，而是多字段组合：

- `parse_status`：文档解析 / 切片状态。
- `embedding_status`：文档向量化整体状态。
- `chunk_count`：当前 chunk 总数。
- `embedded_chunk_count`：成功向量化 chunk 数。
- `latest_task_type`：最近生命周期任务类型。
- `latest_task_status`：最近任务状态。
- `latest_error_message`：最近错误信息。

`GET /api/documents/{documentId}/status` 会聚合这些字段和任务记录，返回 `canReprocess`、`canReembed` 等可操作信息。

## 5. 文档删除闭环

`DELETE /api/documents/{documentId}` 当前会删除：

1. `chunk_embedding`
2. `document_chunk`
3. `task_record`
4. `document`
5. 事务提交后删除本地文件

这样能避免文档删除后仍残留可检索向量或孤立切片。

## 6. 删除 document / chunk / embedding / file 的顺序

删除时应先删关联数据，再删主文档：

- 先删 `chunk_embedding`：避免向量仍能被检索到。
- 再删 `document_chunk`：清理切片文本。
- 再删 `task_record`：清理生命周期任务。
- 再删 `document`：删除主记录。
- 最后删文件：当前实现通过事务提交后的回调删除本地文件，降低数据库回滚但文件已删的风险。

## 7. process force=true 设计

`process force=true` 用于文档内容变化、切片参数变化或旧切片异常时强制重建。

强制处理必须清旧数据：

- 删除旧 `chunk_embedding`。
- 删除旧 `document_chunk`。
- 重新解析和切片。
- 重置 embedding 状态为 `NOT_STARTED`。

原因是旧 chunk 和新 chunk 的语义、序号、ID 都可能不一致，如果不清理旧 embedding，检索结果会混入过期内容。

## 8. embed force=true 设计

`embed force=true` 用于模型切换、历史向量异常或重新生成向量。

当前逻辑会删除当前文档旧的 `chunk_embedding` 后重新生成。这样可以避免不同模型、不同维度或错误向量混在一起。

## 9. 状态聚合 / 错误信息 / 可重试

当前系统通过 `document` 和 `task_record` 共同表达生命周期状态：

- `task_record` 保存任务类型、状态、错误、开始和结束时间。
- `document` 保存当前聚合状态。
- 状态接口返回 `canReprocess`、`canReembed`，用于判断是否允许重试。
- 错误信息会截断并脱敏 API Key 形式内容。

## 10. 数据一致性风险

当前仍需注意：

- 大文档同步处理可能耗时较长。
- Embedding 部分成功时需要通过状态接口判断是否重试。
- 本地文件和数据库不是同一种事务资源，仍可能存在极端不一致风险。
- MySQL 存向量适合 MVP，数据量增大后检索性能会下降。

## 11. 后续可优化方向

- 引入异步任务队列处理 process / embed。
- 对部分失败 chunk 支持单独重试。
- 增加文件存储校验和清理任务。
- 引入对象存储替代本地文件。
- 接入专业向量库并建立文档删除同步机制。
- 增加生命周期相关集成测试。
