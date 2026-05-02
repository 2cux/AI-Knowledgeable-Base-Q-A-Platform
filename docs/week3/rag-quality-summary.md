# RAG 质量优化总结

## 1. RAG 主链路说明

当前 RAG 主链路为：

1. 用户在 `POST /api/chat/ask` 提交 `knowledgeBaseId`、`question`、可选 `topK` 和 `conversationId`。
2. 系统校验知识库属于当前用户。
3. 系统加载当前会话最近消息作为多轮上下文。
4. `RetrievalService` 对问题生成 query embedding，并从当前知识库的 chunk embedding 中检索相关 chunk。
5. 检索结果拆分为 `rawChunks` 和 `effectiveChunks`。
6. 如果无命中或弱命中，直接返回统一拒答。
7. 如果有效命中，构造 RAG Prompt 调用 LLM。
8. `AnswerExtractor` 从 LLM 原始响应中抽取最终答案。
9. 问答结果、命中状态、引用、会话消息写入数据库。

## 2. 文档如何进入检索链路

文档进入检索链路需要经过三步：

- 上传文档：`POST /api/documents/upload-file` 或 `POST /api/documents/upload` 写入 `document`。
- 文档处理：`POST /api/documents/{documentId}/process` 将文本解析并切分到 `document_chunk`。
- 文档向量化：`POST /api/documents/{documentId}/embed` 将 chunk 调用 Embedding API，结果写入 `chunk_embedding.vector_json`。

只有完成切片和 embedding 的 chunk 才能参与后续检索。

## 3. chunk 如何参与检索

`document_chunk` 保存切片文本、文档 ID、知识库 ID、切片序号等信息。检索时系统根据 `knowledgeBaseId` 过滤当前知识库的 chunk embedding，再结合向量相似度排序，返回最相关的 chunk。

当前实现是 MVP 级方案：向量 JSON 存在 MySQL，检索由 Java 侧计算相似度，并不是生产级向量数据库检索。

## 4. embedding 如何生成和使用

- 文档 chunk 的 embedding：由 `DocumentEmbeddingServiceImpl` 遍历 `document_chunk` 调用 `EmbeddingClient` 生成。
- 查询问题的 embedding：由 `QueryEmbeddingService` 在检索时生成。
- 向量存储：`chunk_embedding.vector_json` 保存向量 JSON，`embedding_model` 保存模型名，`status` 保存状态。
- 向量使用：`DatabaseVectorSearchAdapter` 从数据库读取候选向量并做相似度计算。

## 5. rawRetrievedChunkCount / retrievedChunkCount / effectiveChunks 语义

- `rawRetrievedChunkCount`：原始检索返回的 chunk 数量，反映“有没有初步候选”。
- `retrievedChunkCount`：当前问答记录中实际用于回答的有效 chunk 数量。
- `rawChunks`：未经有效性阈值过滤的候选 chunk。
- `effectiveChunks`：通过最低有效分数过滤后，被认为能支撑回答的 chunk。
- `effectiveChunkCount`：有效 chunk 数量，通常与 `retrievedChunkCount` 一致。
- `minEffectiveScore`：有效命中的最低相似度分数，用于观察命中质量。

## 6. matched 的语义

`matched` 表示本次问答是否有足够有效的知识库内容支撑回答。

- `matched=true`：存在有效 chunk，系统可以基于知识片段生成回答。
- `matched=false`：无命中、弱命中或检索不可用，不应把回答当作知识库支撑答案。

## 7. 统一拒答策略

当前拒答策略集中在 `ChatServiceImpl`：

- 原始检索结果为空：返回 `NO_HIT`。
- 有原始结果但有效结果为空：返回 `WEAK_HIT`。
- 检索服务异常：返回 `RETRIEVAL_UNAVAILABLE`。
- LLM 调用或答案抽取失败：返回 `LLM_UNAVAILABLE`。

拒答的价值是：当知识库无法支撑回答时，不让模型自由发挥，从而降低幻觉。

## 8. 答案状态

当前 `AnswerStatus` 包括：

- `SUCCESS`：有效命中且 LLM 成功生成答案。
- `NO_HIT`：没有检索到相关 chunk。
- `WEAK_HIT`：有原始候选，但分数不足以支撑可靠回答。
- `LLM_UNAVAILABLE`：检索成功，但 LLM 不可用或答案抽取失败。
- `RETRIEVAL_UNAVAILABLE`：检索链路异常。

这些状态会写入 `chat_record.answer_status`，方便用户端追溯和管理端运营统计。

## 9. AnswerExtractor 的作用

`AnswerExtractor` 用于从外部 LLM 的原始响应中抽取最终答案。它兼容普通文本、`answer` 字段、`data.answer`、`choices.message.content`、`output_text`、`text`、Responses 风格 `output` 等结构。

它的作用不是提升模型能力，而是提高外部 API 响应结构变化时系统的稳定性，避免把整段 JSON 原样返回给用户。

## 10. citations 的作用

`citations` 保存回答引用的 chunk 信息，包括：

- `chunkId`
- `documentId`
- `knowledgeBaseId`
- `chunkIndex`
- `documentName`
- `score`
- `contentSnippet`

它们会写入 `chat_record.citations_json`，并在问答详情、管理端详情和 message 中展示。面试时可解释为“引用溯源”，用于证明答案基于哪些知识片段。

## 11. RAG 评估样例说明

当前项目已提供最小 RAG 评估材料：

- `docs/rag-eval/rag-eval-knowledge.md`
- `docs/rag-eval/rag-eval-cases.md`
- `docs/rag-eval/rag-eval-cases.json`

这些样例适合用于回归验证：

- 正常命中问题能否返回有效答案和 citations。
- 无关问题是否返回拒答。
- 弱命中问题是否不会被强行回答。

## 12. 当前 RAG 能力边界

- 当前是 MVP 级 RAG，不是生产级知识库搜索系统。
- 向量存储在 MySQL，检索在 Java 侧做相似度计算，适合小规模演示。
- 未接入专业向量数据库。
- 未做复杂 rerank、混合检索、权限到 chunk 粒度、异步批处理队列。
- 答案质量仍依赖文档质量、切片质量、embedding 模型和 LLM 稳定性。

## 13. 后续可优化方向

- 接入向量数据库或 MySQL 向量扩展，提高检索性能。
- 增加 BM25 + 向量混合检索。
- 增加 rerank 模型，提高有效 chunk 质量。
- 细化切片策略，支持标题层级和元数据。
- 增加离线评估脚本和命中率统计。
- 增加异步任务队列处理大文档。
- 增加知识库运营闭环：未命中问题一键转补充文档。
