# 第三周最终验收测试计划

> 适用于 Apifox。除注册 / 登录外，请在请求头加入 `Authorization: Bearer <token>`。管理员接口需要管理员账号 token。示例 ID 如 `knowledgeBaseId`、`documentId`、`chatRecordId`、`conversationId` 需使用前置接口真实返回值替换。

## 1. 注册

- 测试目标：创建普通用户。
- 请求方法：POST
- 请求路径：`/auth/register`
- 前置条件：用户名未被注册。
- 请求体示例：

```json
{
  "username": "demo_user",
  "password": "123456",
  "confirmPassword": "123456",
  "email": "demo@example.com"
}
```

- 预期结果：返回用户 ID。
- 常见失败原因：用户名重复、密码长度不合法、两次密码不一致。
- 通过标准：响应成功，数据库 `user` 有新记录。

## 2. 登录

- 测试目标：获取 JWT。
- 请求方法：POST
- 请求路径：`/auth/login`
- 前置条件：用户已注册。
- 请求体示例：

```json
{
  "username": "demo_user",
  "password": "123456"
}
```

- 预期结果：返回 token。
- 常见失败原因：用户名或密码错误。
- 通过标准：后续接口使用 `Authorization: Bearer <token>` 可访问。

## 3. 创建知识库

- 测试目标：创建一个当前用户拥有的知识库。
- 请求方法：POST
- 请求路径：`/api/kb`
- 前置条件：已登录。
- 请求体示例：

```json
{
  "name": "Java 面试知识库",
  "description": "用于 RAG 问答验收的示例知识库"
}
```

- 预期结果：返回知识库 ID。
- 常见失败原因：未登录、名称为空、名称过长。
- 通过标准：返回 `id`，后续文档可绑定该知识库。

## 4. 上传 txt / md 文档

- 测试目标：上传真实文档文件。
- 请求方法：POST
- 请求路径：`/api/documents/upload-file`
- 前置条件：已登录，已有 `knowledgeBaseId`。
- 请求参数 / 请求体示例：`multipart/form-data`

```text
knowledgeBaseId=1
file=@rag-demo.md
fileName=rag-demo.md
```

- 预期结果：返回 `documentId`、`fileName`、`fileType`、`parseStatus=NOT_STARTED`。
- 常见失败原因：未登录、知识库不存在或无权限、文件为空、文件类型不支持、超过 20MB。
- 通过标准：文档记录创建成功，文件存储路径存在。

## 5. 文档解析 process

- 测试目标：将文档解析并切分为 chunk。
- 请求方法：POST
- 请求路径：`/api/documents/{documentId}/process`
- 前置条件：文档属于当前用户。
- 请求体示例：

```json
{
  "chunkSize": 500,
  "overlap": 50,
  "force": false
}
```

- 预期结果：返回 `chunkCount > 0`、`parseStatus=SUCCESS`。
- 常见失败原因：文档不存在、无权限、`overlap >= chunkSize`、文档内容为空、重复处理。
- 通过标准：`GET /api/documents/{documentId}/chunks` 能查到 chunk。

## 6. 文档 embedding

- 测试目标：为 chunk 生成向量。
- 请求方法：POST
- 请求路径：`/api/documents/{documentId}/embed`
- 前置条件：文档已 process 成功；Embedding API Key 或本地配置可用。
- 请求体示例：

```json
{
  "force": false
}
```

- 预期结果：返回 `successCount`，`failedCount` 为 0 或可解释。
- 常见失败原因：文档未处理、Embedding API Key 未配置、外部服务不可用。
- 通过标准：`GET /api/documents/{documentId}/embedding-status` 显示成功数等于 chunk 数，或部分失败可重试。

## 7. 用户提问 chat/ask

- 测试目标：跑通 RAG 问答。
- 请求方法：POST
- 请求路径：`/api/chat/ask`
- 前置条件：知识库下至少有一个已 embedding 成功的文档。
- 请求体示例：

```json
{
  "knowledgeBaseId": 1,
  "question": "RAG 是什么？",
  "topK": 5
}
```

- 预期结果：返回 `chatRecordId`、`conversationId`、`answer`、`answerStatus`、`matched`、`citations`。
- 常见失败原因：未登录、知识库不存在、文档未处理、文档未 embedding、LLM 不可用。
- 通过标准：正常命中时 `answerStatus=SUCCESS`，`matched=true`，`citations` 非空。

## 8. 查看问答详情

- 测试目标：验证用户端追溯字段。
- 请求方法：GET
- 请求路径：`/api/chat/records/{id}`
- 前置条件：已有 `chatRecordId`。
- 请求参数：路径参数 `id=<chatRecordId>`。
- 预期结果：返回问题、答案、`answerStatus`、`matched`、`retrievedChunkCount`、`topK`、`citations`。
- 常见失败原因：记录不存在、访问他人记录。
- 通过标准：字段与 `chat/ask` 返回一致。

## 9. 查看会话列表

- 测试目标：验证会话聚合。
- 请求方法：GET
- 请求路径：`/api/chat/conversations`
- 前置条件：已完成至少一次问答。
- 请求参数示例：`?knowledgeBaseId=1&page=1&size=10`
- 预期结果：返回会话列表，包含 `conversationId`、`messageCount`、`lastQuestion`、`lastAnswerPreview`。
- 常见失败原因：未登录、`page` 或 `size` 非法。
- 通过标准：能看到刚才问答产生的会话。

## 10. 查看会话详情

- 测试目标：验证 message 双写。
- 请求方法：GET
- 请求路径：`/api/chat/conversations/{conversationId}`
- 前置条件：已有 `conversationId`。
- 请求参数：路径参数 `conversationId=<chat/ask 返回值>`。
- 预期结果：返回 user 和 assistant 两类消息。
- 常见失败原因：会话不存在、访问他人会话。
- 通过标准：消息中包含刚才的问题和答案，assistant 消息带 `chatRecordId`。

## 11. 提交反馈

- 测试目标：提交 LIKE / DISLIKE。
- 请求方法：POST
- 请求路径：`/api/chat/records/{id}/feedback`
- 前置条件：已有当前用户自己的问答记录。
- 请求体示例：

```json
{
  "feedbackType": "DISLIKE",
  "comment": "回答引用不够准确"
}
```

- 预期结果：提交成功。
- 常见失败原因：记录不存在、访问他人记录、`feedbackType` 不是 `LIKE` 或 `DISLIKE`、评论超过 500 字。
- 通过标准：管理端反馈列表可查到。

## 12. 管理员查看问答详情

- 测试目标：验证管理员可看完整问答追溯。
- 请求方法：GET
- 请求路径：`/api/admin/chat/records/{id}`
- 前置条件：使用管理员 token，已有问答记录。
- 请求参数：路径参数 `id=<chatRecordId>`。
- 预期结果：返回用户 ID、知识库 ID、问题、答案、命中状态、raw / effective 数量、citations。
- 常见失败原因：非管理员、记录不存在。
- 通过标准：管理员可查看，普通用户访问被拒绝。

## 13. 管理员查看未命中问题

- 测试目标：验证未命中治理基础能力。
- 请求方法：GET
- 请求路径：`/api/admin/chat/missed-questions`
- 前置条件：管理员 token；至少制造一次无关问题导致 `matched=false`。
- 请求参数示例：`?knowledgeBaseId=1&page=1&size=10`
- 预期结果：返回未命中问题列表。
- 常见失败原因：非管理员、时间参数格式错误、`startTime` 晚于 `endTime`。
- 通过标准：无关问题出现在列表中。

## 14. 管理员查看反馈列表

- 测试目标：验证反馈运营查询。
- 请求方法：GET
- 请求路径：`/api/admin/chat/feedback`
- 前置条件：管理员 token；已有 LIKE / DISLIKE。
- 请求参数示例：`?knowledgeBaseId=1&rating=DISLIKE&page=1&size=10`
- 预期结果：返回反馈、评论、问题和答案预览。
- 常见失败原因：`rating` 非 `LIKE/DISLIKE`、非管理员、`startTime > endTime`。
- 通过标准：能查到第 11 步提交的反馈。

## 15. 管理员查看热门问题

- 测试目标：验证 TopN 聚合。
- 请求方法：GET
- 请求路径：`/api/admin/chat/hot-questions`
- 前置条件：管理员 token，存在多条问答记录。
- 请求参数示例：`?knowledgeBaseId=1&limit=10`
- 预期结果：返回问题、次数和最近提问时间。
- 常见失败原因：非管理员、limit 非法时会被归一化或限制到最大值，需以代码为准。
- 通过标准：重复问题排名靠前。

## 16. 管理员查看基础统计

- 测试目标：验证统计指标。
- 请求方法：GET
- 请求路径：`/api/admin/chat/stats`
- 前置条件：管理员 token，存在问答和反馈数据。
- 请求参数示例：`?knowledgeBaseId=1`
- 预期结果：返回总问答数、命中数、未命中数、命中率、反馈数、点赞 / 点踩数。
- 常见失败原因：非管理员、时间参数格式错误、`startTime > endTime`。
- 通过标准：统计值与测试数据基本一致。

## 17. 文档重处理

- 测试目标：验证 `process force=true` 清理旧 chunk 并重建。
- 请求方法：POST
- 请求路径：`/api/documents/{documentId}/process`
- 前置条件：文档已处理过。
- 请求体示例：

```json
{
  "chunkSize": 300,
  "overlap": 30,
  "force": true
}
```

- 预期结果：重新生成 chunk，文档 `embeddingStatus` 回到 `NOT_STARTED`。
- 常见失败原因：文档处理中、`overlap >= chunkSize`、无权限。
- 通过标准：旧 embedding 被清理，需要重新 embed。

## 18. 文档重新 embedding

- 测试目标：验证 `embed force=true` 清旧向量并重建。
- 请求方法：POST
- 请求路径：`/api/documents/{documentId}/embed`
- 前置条件：文档已 process 成功。
- 请求体示例：

```json
{
  "force": true
}
```

- 预期结果：重新生成 `chunk_embedding`。
- 常见失败原因：文档未处理、Embedding 服务不可用。
- 通过标准：embedding 状态成功，旧错误状态不再影响新结果。

## 19. 文档删除

- 测试目标：验证删除闭环。
- 请求方法：DELETE
- 请求路径：`/api/documents/{documentId}`
- 前置条件：文档属于当前用户。
- 请求参数：路径参数 `documentId=<documentId>`。
- 预期结果：删除成功。
- 常见失败原因：文档不存在、无权限。
- 通过标准：文档详情查不到，chunk / embedding / task 不再残留，文件被清理。

## 关键异常流清单

| 异常流 | 建议验证方式 | 预期 |
|---|---|---|
| 未登录 | 不传 Authorization 访问 `/api/kb` | 被拒绝 |
| 无权限 | 用户 A 访问用户 B 的知识库 / 文档 / 会话 | 返回无权限或资源不存在 |
| 资源不存在 | 使用不存在的 `documentId`、`chatRecordId` | 返回业务错误 |
| 参数错误 | `page=0`、`size=0`、`topK=0`、`overlap>=chunkSize` | 返回参数错误 |
| 文档未处理就提问 | 上传后不 process / embed 直接问 | 通常无命中或检索无结果，需以实际链路为准 |
| 文档未 embedding 就提问 | process 后不 embed 直接问 | 通常 `NO_HIT` 或检索无结果 |
| 未命中拒答 | 问知识库外问题 | `matched=false`，`answerStatus=NO_HIT` 或 `WEAK_HIT` |
| LLM 不可用兜底 | 移除 / 配错 LLM API Key 后命中提问 | `answerStatus=LLM_UNAVAILABLE`，有 citations |
| citations 为空 | 未命中 / 弱命中 | `citations=[]` 且拒答 |
| page / size 非法 | `page=0` 或 `size=101` | 参数错误或归一化，需以接口为准 |
| startTime 晚于 endTime | 管理端运营接口传反时间 | 返回 `startTime cannot be later than endTime` |
