# 第三周 RAG 质量优化设计

## 1. 设计目标

本设计文档用于给出第三周 RAG 质量优化的最小可行技术方案，目标是：

- 在不推翻现有 Spring Boot 单体结构的前提下，提升检索和问答质量稳定性。
- 明确当前基础版检索架构的能力边界。
- 为后续 rerank、Prompt 配置化、质量评估平台预留演进空间。

## 2. 当前实现基线

### 2.1 当前真实链路

当前问答主链路为：

```text
chat/ask
  -> RetrievalService
  -> QueryEmbeddingService
  -> DatabaseVectorSearchAdapter
  -> AnswerGeneratorService
  -> chat_record 落库
```

### 2.2 当前真实问题

- 检索依赖 `chunk_embedding.vector_json`，通过 Java 计算余弦相似度，不具备正式向量检索平台的召回与扩展能力。
- 当前有效命中只靠固定阈值判断。
- 当前 LLM 客户端适配层需要进一步明确“如何从上游响应中抽取最终答案文本”。
- 当前问答详情未完整暴露追溯信息。

## 3. 总体设计原则

- 保持现有接口主路径不变。
- 增量增强 `retrieval -> answer -> trace` 三段。
- 先做最小可控质量闭环，再预留更高级能力。
- 不引入过早复杂基础设施。

## 4. 检索质量设计

## 4.1 当前架构边界说明

第三周文档必须明确：

- 当前检索仍是“**数据库落向量 JSON + 应用层计算相似度**”。
- 该实现适合作为 Alpha/Beta 过渡期的基础版方案。
- 该实现不等价于正式版向量检索基础设施。

这一定义需要在文档、验收和对外项目判断中保持一致，避免误判成熟度。

## 4.2 有效命中判定模型

建议将检索分为两个层次：

- 召回结果：向量搜索返回的原始 topK chunk。
- 有效结果：经过阈值过滤后，真正可进入回答上下文和 citations 的 chunk。

### 建议规则

- `topK` 继续表示召回上限。
- 增加最小有效分数阈值 `minEffectiveScore`。
- 如需要，增加最小有效命中数 `minEffectiveChunkCount` 的策略预留。

### 第三周最小实现

- 第一阶段仍使用当前单阈值方式，但改为可配置。
- 配置默认值可以沿用当前逻辑附近值，例如 `0.05`。
- 在服务层明确区分：
- `retrievedChunkCountRaw`
- `retrievedChunkCountEffective`

第三周不要求一定把两个值都对外暴露，但内部语义必须清晰。

## 4.3 rerank 预留设计

第三周不强制接入 rerank 服务，但应预留适配层：

- 在 `RetrievalService` 与 `AnswerGeneratorService` 之间预留“结果重排”概念。
- 设计上可抽象为：
- `RawRetrievalCandidates`
- `EffectiveRetrievalCandidates`

### 演进建议

- 当前阶段：`EffectiveRetrievalCandidates = threshold filter(raw candidates)`
- 后续阶段：`EffectiveRetrievalCandidates = rerank(filter(raw candidates))`

这样后续引入 rerank 时，不需要推翻现有问答接口和返回结构。

## 5. 拒答策略设计

## 5.1 场景划分

第三周建议把拒答分为三类：

- `NO_HIT`
- `WEAK_HIT`
- `LLM_UNAVAILABLE`

### 语义说明

- `NO_HIT`：召回为空，或全部低于有效阈值。
- `WEAK_HIT`：有命中但证据不足以支撑可信回答。
- `LLM_UNAVAILABLE`：检索命中但模型调用失败。

### 第三周最小实现建议

- 不一定对外暴露枚举字段，但内部应统一这三类分支。
- `matched` 建议只在 `NO_HIT/WEAK_HIT` 场景下为 `false`。
- LLM 不可用时：
- 如果已有可靠引用片段但不能生成答案，可返回明确降级提示。
- 该场景不应伪装成正常问答成功。

## 5.2 拒答文案策略

建议统一由答案生成层输出，而不是在多处散落硬编码。

可分为：

- 知识不足拒答文案。
- 模型不可用降级文案。
- 保守回答提示文案。

第三周不要求做后台配置，但应集中收口到单一组件或常量定义位置。

## 6. 答案输出抽取设计

## 6.1 当前问题

当前真实 LLM 已接入，但适配层和答案生成层之间需要进一步明确：

- 上游响应的结构化内容如何转成最终展示文本。
- 空响应、异常响应、非预期响应如何兜底。

## 6.2 第三周目标设计

建议把答案生成分为两步：

```text
LLM 原始响应
  -> AnswerExtractor
  -> 最终 answer 文本
```

### 设计要点

- `LlmClient` 负责第三方调用。
- `AnswerExtractor` 负责从第三方响应结构中提取最终文本。
- `AnswerGeneratorService` 负责：
- 组装 Prompt。
- 调用 LLM。
- 结合命中情况和异常情况给出最终 answer。

### 兜底逻辑

- 提取为空：按拒答或降级处理。
- 响应格式异常：记录日志并走降级。
- 文本过长：可截断或按规则清洗。

## 6.3 回答格式规范

第三周建议统一回答格式要求：

- 中文优先。
- 简洁直接。
- 不编造。
- 回答尽量不包含“模型视角”的冗余描述。
- 不直接暴露原始 JSON 或调试信息。

## 7. citations 详情补齐设计

## 7.1 当前问题

`chat/ask` 能返回 citations，但问答详情追溯不完整。

## 7.2 第三周设计

继续沿用 `chat_record.citations_json` 字段，不新增引用表。

### 用户问答详情返回建议

- `id`
- `knowledgeBaseId`
- `conversationId`
- `question`
- `answer`
- `matched`
- `retrievedChunkCount`
- `topK`
- `citations`
- `createdAt`

### 管理端问答详情返回建议

- `id`
- `userId`
- `knowledgeBaseId`
- `conversationId`
- `question`
- `answer`
- `matched`
- `retrievedChunkCount`
- `topK`
- `citations`
- `createdAt`

### 历史数据兼容

- 对历史 `citations_json` 为空或格式异常的数据，返回空列表并记录解析失败日志。
- 不因单条旧数据异常导致详情接口整体失败。

## 8. Prompt 治理设计

## 8.1 最小结构

第三周保留当前 PromptBuilder 模式，但明确结构：

- system：角色与拒答规则。
- history：最近若干轮上下文。
- chunks：知识片段。
- question：当前问题。

## 8.2 上下文长度控制

建议继续限制：

- 历史轮数。
- 每条历史文本长度。
- 每个 chunk 长度。
- 总体上下文长度上限。

第三周不强制做 token 级精算，但要在设计文档中明确截断策略。

## 9. 质量评估设计

## 9.1 最小评估集

建议建立固定评估样例表，至少包括：

- 命中型问题。
- 边缘命中问题。
- 明确未命中问题。
- 文档外问题。
- 多轮追问问题。

## 9.2 验收字段

每条样例建议记录：

- 归属知识库。
- 测试文档。
- 输入问题。
- 期望命中状态。
- 期望引用数量范围。
- 期望回答行为。

## 9.3 第三周不做

- 不做自动化评测平台。
- 不做在线 A/B 实验。
- 不做复杂质量评分系统。

## 10. API 影响

第三周建议影响如下接口：

- `POST /api/chat/ask`
- `GET /api/chat/records/{id}`
- `GET /api/admin/chat/records/{id}`

### 兼容要求

- 不更改主路径。
- 字段新增优先向后兼容。
- 对旧数据保持可读。

## 11. 风险与注意事项

- 当前检索基础设施较轻，不适合过度承诺召回质量。
- 质量设计必须和当前数据模型兼容，不能引入过重的独立引用体系。
- 如果答案抽取改造不当，可能影响现有真实问答主链路稳定性，需配合固定样例回归。

## 12. 第三周完成标准

- 检索有效命中策略文档化并落到实现。
- 拒答场景分类明确。
- 答案输出抽取不再依赖原始 JSON 直接透传。
- 用户和管理端问答详情补齐追溯字段。
- 最小质量评估样例集建立完成。
