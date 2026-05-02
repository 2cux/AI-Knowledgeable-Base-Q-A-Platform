# 简历项目描述

## 1. 项目名称

AI 知识库问答平台

## 2. 项目简介

基于 Spring Boot 的 AI 知识库问答平台，支持用户创建知识库、上传文档、文档切片、Embedding 向量化、RAG 问答、引用溯源、多轮会话、用户反馈和管理端基础运营统计。项目当前定位为 Java 后端实习展示项目，重点体现完整业务闭环和 RAG 后端链路设计。

## 3. 技术栈

- Java
- Spring Boot
- Spring Security
- JWT
- MyBatis-Plus
- MySQL
- Flyway
- LLM API
- Embedding API
- RAG
- Apifox

未在简历中建议写：Redis、RabbitMQ、微服务、高并发、分布式、生产级向量数据库，除非后续代码实际实现。

## 4. 个人职责

- 负责后端接口设计与实现。
- 负责数据库表设计和 Flyway 迁移脚本维护。
- 负责文档上传、解析、切片、Embedding 和 RAG 问答链路。
- 负责会话系统、问答记录、引用追溯和用户反馈。
- 负责管理端基础运营接口和项目技术文档整理。

## 5. 核心功能

- 用户注册 / 登录 / JWT 鉴权。
- 知识库和文档管理。
- txt / md 文档上传、解析、切片。
- 文档 chunk Embedding 入库。
- 基于 RAG 的知识库问答。
- `NO_HIT` / `WEAK_HIT` / `LLM_UNAVAILABLE` 等拒答和兜底策略。
- citations 引用溯源。
- conversation / message 多轮会话。
- 用户反馈和管理端运营统计。
- 文档重处理、重新向量化和删除闭环。

## 6. 项目亮点

- 从文档上传到 RAG 问答形成完整闭环。
- 问答结果可追溯到 chunk 和文档，降低大模型幻觉风险。
- 引入统一拒答策略，区分无命中、弱命中和 LLM 不可用。
- 会话系统与问答审计记录分离，兼顾多轮上下文和运营追溯。
- 文档生命周期支持重处理、重新向量化、状态聚合和关联数据删除。
- 管理端提供未命中问题、反馈、热门问题和基础统计，体现运营闭环意识。

## 7. 简历 bullet points

- 使用 Spring Boot、Spring Security 和 JWT 实现用户注册登录、接口鉴权与管理员权限校验，保证知识库、文档、会话和问答记录按用户隔离访问。
- 设计知识库文档处理链路，支持 txt / md 文档上传、文本解析、chunk 切片、状态聚合和 Flyway 表结构迁移，为后续 RAG 检索提供结构化数据基础。
- 基于 Embedding API 实现文档 chunk 向量化，将向量 JSON 存储到 MySQL，并通过 Java 侧相似度计算完成 MVP 级语义检索。
- 实现 RAG 问答主链路，支持检索结果 raw / effective 拆分、引用溯源 citations、`NO_HIT` / `WEAK_HIT` / `LLM_UNAVAILABLE` 等拒答状态，降低无依据回答风险。
- 设计 conversation / message 会话模型，将多轮上下文与 chat_record 审计记录分离，实现会话列表、会话详情和追问上下文加载。
- 实现管理端基础运营接口，支持问答详情、未命中问题、用户反馈、热门问题 TopN 和命中率统计，为知识库内容迭代提供数据依据。

## 8. 面试可展开讲的点

- 文档如何从上传变成可检索向量。
- 为什么当前使用 MySQL 存向量，边界是什么。
- RAG 如何降低幻觉。
- citations 如何实现。
- 为什么需要 `conversation` 和 `message`，而不是只用 `chat_record`。
- `process force=true` 和 `embed force=true` 解决什么问题。
- 文档删除为什么要先删 chunk 和 embedding。
- 管理端运营数据如何反哺知识库。

## 9. 不建议写进简历的点

- 不写高并发、分布式、微服务。
- 不写 Redis / RabbitMQ，除非实际接入。
- 不写生产级向量数据库，当前是 MySQL + Java cosine MVP。
- 不写完整运营后台，当前是管理端基础运营接口。
- 不写复杂 Agent、多模态知识库、自动知识更新，除非后续实现。
