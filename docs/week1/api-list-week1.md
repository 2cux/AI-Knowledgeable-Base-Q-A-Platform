# 第 1 批接口清单（第 1 周）

## 1. 文档目标

本文档用于整理第 1 周需要先定义的接口清单，重点是把单体 Spring Boot MVP 的核心接口边界确定下来。

当前阶段只做接口级别设计，不展开请求响应字段的全部细节，也不直接进入复杂 AI 链路实现。

## 2. 接口设计原则

- 只覆盖第 1 版 MVP 核心闭环
- 接口数量控制在个人项目可落地范围内
- 优先 REST 风格
- 命名清晰，便于后续 Swagger 文档生成
- 先满足后端单体开发节奏，不做过度抽象

## 3. 模块接口总览

第 1 批接口按模块划分如下：

- 认证模块：`auth`
- 知识库模块：`kb`
- 文档模块：`document`
- 问答模块：`qa`
- 会话模块：`conversation`
- 任务模块：`task`
- 管理模块：`admin`

## 4. 接口清单

### 4.1 auth

#### `POST /api/auth/register`

用途：

- 用户注册

#### `POST /api/auth/login`

用途：

- 用户登录
- 返回 JWT

#### `GET /api/auth/me`

用途：

- 获取当前登录用户信息

### 4.2 kb

#### `POST /api/kb`

用途：

- 创建知识库

#### `GET /api/kb`

用途：

- 分页查询当前用户可见知识库列表

#### `GET /api/kb/{id}`

用途：

- 查询知识库详情

### 4.3 document

#### `POST /api/documents/upload`

用途：

- 上传文档
- 绑定到指定知识库

#### `GET /api/documents`

用途：

- 分页查询文档列表
- 支持按知识库筛选

#### `GET /api/documents/{id}`

用途：

- 查询文档详情

#### `POST /api/documents/{id}/parse`

用途：

- 手动触发文档解析/切片任务

说明：

- 第 1 周可以先定义接口和任务记录，不要求全部底层处理实现完成

### 4.4 qa

#### `POST /api/qa/ask`

用途：

- 发起一次基础问答
- 关联知识库和会话

说明：

- 第 1 周先定义好请求结构
- 引用信息与未命中信息先保留返回结构和记录位置

### 4.5 conversation

#### `POST /api/conversations`

用途：

- 创建新会话

#### `GET /api/conversations`

用途：

- 查询当前用户会话列表

#### `GET /api/conversations/{id}`

用途：

- 查询会话详情

#### `GET /api/conversations/{id}/messages`

用途：

- 查询指定会话的消息历史

### 4.6 task

#### `GET /api/tasks/{id}`

用途：

- 查询单个任务记录详情

#### `GET /api/tasks`

用途：

- 分页查询任务记录
- 支持按业务类型、状态筛选

### 4.7 admin

#### `GET /api/admin/overview`

用途：

- 查询基础统计信息
- 用于管理端首页简单概览

#### `GET /api/admin/documents`

用途：

- 管理端查询文档列表

#### `GET /api/admin/conversations`

用途：

- 管理端查询问答会话记录

## 5. 第 1 周建议优先级

建议按以下顺序推进接口设计和骨架开发：

1. `auth`
2. `kb`
3. `document`
4. `conversation`
5. `qa`
6. `task`
7. `admin`

这样做的原因是：

- 先把登录和基础资源管理打通
- 再补会话和问答主链路
- 最后补任务与管理端查询接口

## 6. 建议的返回结构

第 1 周接口建议统一返回结构：

- `code`
- `message`
- `data`

分页接口建议统一包含：

- `list`
- `total`
- `pageNum`
- `pageSize`

## 7. 第 1 周不纳入的接口

以下接口暂不进入第 1 周：

- 向量检索管理接口
- Embedding 重建接口
- 模型配置接口
- 多知识库路由接口
- 审核流接口
- 复杂权限管理接口
- Tool Calling 相关接口

## 8. 结论

第 1 批接口清单只服务于“登录、知识库、文档、问答、会话、任务、基础管理”这条最小主链路，确保第 2 周可以直接进入骨架编码和接口实现阶段。
