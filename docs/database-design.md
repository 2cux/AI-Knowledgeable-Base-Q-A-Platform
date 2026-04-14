# 数据库设计（第 1 周）

## 1. 文档目标

本文档用于定义第 1 周 MVP 的核心数据库表结构，只覆盖当前最小业务闭环，不提前引入复杂权限、审核流、版本流和向量检索细节。

数据库选型固定为 MySQL 8。

## 2. 设计原则

- 只保留当前 MVP 必需的核心表
- 字段设计以“够用、清晰、易扩展”为原则
- 先支持单体 Spring Boot 场景
- 先不处理复杂多租户和细粒度权限
- 与 AI 深度能力相关的字段只做最小预留

## 3. 核心表清单

第 1 周核心表共 7 张：

- `user`
- `knowledge_base`
- `document`
- `document_chunk`
- `conversation`
- `message`
- `task_record`

## 4. 表关系总览

### 4.1 关系说明

- 一个用户可以创建多个知识库
- 一个知识库下可以上传多个文档
- 一个文档可以切分出多个片段
- 一个用户可以发起多个会话
- 一个会话下可以保存多条消息
- 一个文档可以对应多个处理任务

### 4.2 简化 ER 关系

```text
user 1 --- n knowledge_base
user 1 --- n conversation
knowledge_base 1 --- n document
knowledge_base 1 --- n conversation
document 1 --- n document_chunk
document 1 --- n task_record
conversation 1 --- n message
```

## 5. 表设计说明

### 5.1 user

表用途：

- 存储系统用户基础信息和登录信息

建议字段：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| username | varchar(64) | 登录用户名，唯一 |
| password_hash | varchar(255) | 密码哈希 |
| nickname | varchar(64) | 用户昵称 |
| role | varchar(32) | 角色，先支持 `USER` / `ADMIN` |
| status | tinyint | 状态，1 启用，0 禁用 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

说明：

- 第 1 周只保留最基础角色字段
- 不做独立角色表和权限表

### 5.2 knowledge_base

表用途：

- 存储知识库基础信息

建议字段：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| name | varchar(128) | 知识库名称 |
| description | varchar(500) | 知识库描述 |
| owner_id | bigint | 创建人 ID |
| status | tinyint | 状态，1 正常，0 禁用 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

说明：

- 第 1 周按单拥有者模型处理
- 暂不设计知识库成员关系表

### 5.3 document

表用途：

- 存储上传文档的元数据和处理状态

建议字段：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| knowledge_base_id | bigint | 所属知识库 ID |
| file_name | varchar(255) | 文件名 |
| file_type | varchar(32) | 文件类型，如 pdf、docx、txt |
| file_size | bigint | 文件大小，单位字节 |
| storage_path | varchar(500) | 存储路径 |
| parse_status | varchar(32) | 解析状态，如 `UPLOADED`、`PARSING`、`DONE`、`FAILED` |
| created_by | bigint | 上传人 ID |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

说明：

- 第 1 周只关心元数据和状态，不做版本流

### 5.4 document_chunk

表用途：

- 存储文档切片后的文本片段

建议字段：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| document_id | bigint | 所属文档 ID |
| knowledge_base_id | bigint | 所属知识库 ID |
| chunk_index | int | 片段序号 |
| content | text | 片段内容 |
| token_count | int | 片段长度估算，可为空 |
| created_at | datetime | 创建时间 |

说明：

- 第 1 周先不加入 embedding 向量字段
- 如果后续接入向量检索，再补充扩展表或额外字段

### 5.5 conversation

表用途：

- 存储用户会话信息

建议字段：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| user_id | bigint | 用户 ID |
| knowledge_base_id | bigint | 当前会话关联知识库 ID |
| title | varchar(255) | 会话标题 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

说明：

- 标题可先由前端传入或后端简单生成

### 5.6 message

表用途：

- 存储会话中的用户问题和系统回答

建议字段：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| conversation_id | bigint | 会话 ID |
| role | varchar(32) | 角色，如 `USER`、`ASSISTANT` |
| content | text | 消息内容 |
| message_type | varchar(32) | 消息类型，如 `QUESTION`、`ANSWER` |
| quote_json | text | 引用片段 JSON，先预留 |
| created_at | datetime | 创建时间 |

说明：

- 第 1 周可以先把引用信息作为 JSON 文本存储
- 不单独拆引用表，避免过早复杂化

### 5.7 task_record

表用途：

- 记录文档解析、切片等后台任务状态

建议字段：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| task_type | varchar(32) | 任务类型，如 `PARSE`、`CHUNK` |
| biz_type | varchar(32) | 业务类型，如 `DOCUMENT` |
| biz_id | bigint | 业务主键 ID |
| status | varchar(32) | 状态，如 `PENDING`、`RUNNING`、`SUCCESS`、`FAILED` |
| error_message | varchar(1000) | 失败原因 |
| retry_count | int | 重试次数 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

说明：

- 第 1 周先用任务表描述异步处理状态
- 是否接 MQ、定时任务、线程池，后续再定

## 6. 索引建议

第 1 周建议只加最基本索引：

- `user.username` 唯一索引
- `knowledge_base.owner_id` 普通索引
- `document.knowledge_base_id` 普通索引
- `document.created_by` 普通索引
- `document_chunk.document_id` 普通索引
- `conversation.user_id` 普通索引
- `conversation.knowledge_base_id` 普通索引
- `message.conversation_id` 普通索引
- `task_record.biz_type + biz_id` 联合索引

## 7. 第 1 周不纳入的表

以下表暂不进入第 1 周设计：

- 角色表
- 权限表
- 用户角色关联表
- 知识库成员表
- 文档版本表
- 审核记录表
- 模型配置表
- 向量索引表
- Embedding 结果表

## 8. 结论

第 1 周数据库设计只服务于最小闭环：

- 用户登录
- 知识库管理
- 文档上传
- 文档切片存储
- 基础问答
- 会话历史
- 任务状态记录

这样既能保证结构完整，又不会因为 AI 深水区设计过早膨胀。
