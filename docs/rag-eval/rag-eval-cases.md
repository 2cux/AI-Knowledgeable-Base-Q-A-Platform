# 最小 RAG 评估集 / 回归样例

## 1. 评估目标

本评估集用于在每次修改 RAG、Chat、Conversation、Document 生命周期相关代码后，快速确认主链路没有被破坏。当前阶段只做 MVP 级人工回归，不建设复杂评测平台，不做压测，不引入新技术栈。

重点验证：

- 文档上传、解析、切片、embedding 后可以被稳定检索。
- `matched` 能反映是否存在足够可信的有效命中。
- `retrievedChunkCount` 与 `rawRetrievedChunkCount` 的语义不混淆。
- 无命中、弱命中、LLM 不可用等异常流有统一且可解释的拒答或降级表现。
- `citations` 可用于用户端和管理端追溯命中的文档与 chunk。
- `AnswerExtractor` 能从真实 LLM 返回结构中抽取最终可展示答案。

## 2. 测试前置条件

1. 后端服务已启动，并连接到可用数据库。
2. 当前测试账号已登录，并能创建或访问一个独立知识库。
3. 已上传 `docs/week3/rag-eval-knowledge.md` 到该知识库。
4. 上传后需等待文档状态进入可检索状态，不能只看到“上传成功”就开始测试。
5. 文档解析、切片、embedding 任务应执行完成。
6. 测试前建议清空或区分历史问答会话，避免误判旧记录。
7. 正常用例需要 LLM 服务可用；LLM 不可用降级用例需要临时关闭或配置错误的 LLM 服务。

## 3. 测试知识库内容要求

测试知识库至少包含固定知识源 `rag-eval-knowledge.md`，内容应覆盖：

- 文档上传成功不代表解析完成。
- 文档解析、切片、embedding 的关系。
- `matched` 的语义。
- `retrievedChunkCount` 与 `rawRetrievedChunkCount` 的区别。
- `citations` 的作用。
- `AnswerExtractor` 的职责。
- `NO_HIT`、`WEAK_HIT`、`LLM_UNAVAILABLE` 的拒答或降级策略。

测试知识库不应混入大量无关文档，否则可能影响命中判断。若必须混用真实数据，建议单独创建“RAG 回归测试知识库”。

## 4. RAG 回归测试用例

| 用例编号 | 场景类型 | 测试问题 | 预期 matched | 预期 retrievedChunkCount | 是否要求 citations | 预期答案关键词 | 验收重点 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RAG-HIT-001 | 明确命中 | 文档上传成功后，是否代表已经完成解析并可以立即问答？ | true | >= 1 | 是 | 上传成功、不代表、解析、embedding | 能命中固定知识源，并回答“上传成功不等于可检索”。 |
| RAG-HIT-002 | 明确命中 | matched 字段表示什么？ | true | >= 1 | 是 | 有效命中、可信、不是 LLM 是否返回文本 | `matched` 语义稳定，不被回答文本存在与否混淆。 |
| RAG-MULTI-003 | 多 chunk 命中 | 文档解析、切片和 embedding 之间是什么关系？ | true | >= 2 | 是 | 解析、切片、embedding、向量检索 | 可以召回多个相关片段，答案综合多个 chunk。 |
| RAG-WEAK-004 | 弱命中 | 如果只检索到一点相似内容，但证据不足，系统应该怎么处理？ | false | >= 0 | 否 | WEAK_HIT、证据不足、保守拒答 | 弱命中不应包装成确定答案。 |
| RAG-NOHIT-005 | 无命中 | 平台是否支持用区块链智能合约自动发放工资？ | false | 0 | 否 | 未找到、无法回答、知识库 | 文档无关问题应拒答，不应编造。 |
| RAG-OUT-006 | 文档外问题 | 今天上海天气怎么样？ | false | 0 | 否 | 知识库、无法回答、未包含 | 通用外部事实问题不应脱离知识库回答。 |
| RAG-CITE-007 | 引用追溯 | citations 在问答结果中有什么作用？ | true | >= 1 | 是 | documentId、chunkId、chunkIndex、score、contentSnippet | 用户端和管理端详情中可追溯 citation 字段。 |
| RAG-EXTRACT-008 | AnswerExtractor 输出抽取 | AnswerExtractor 的职责是什么？ | true | >= 1 | 是 | 抽取、最终答案、真实 LLM、可展示文本 | 返回答案应为最终文本，不应透传上游原始 JSON。 |
| RAG-LLM-009 | LLM 不可用降级 | 在 LLM 服务不可用时，命中文档后系统应该如何返回？ | true | >= 1 | 是 | LLM_UNAVAILABLE、降级、模型不可用、稍后重试 | 检索命中仍可追溯，但答案应明确降级，不能伪装成功。 |

## 5. 人工 Apifox 测试步骤

1. 创建或选择独立知识库，例如“RAG 回归测试知识库”。
2. 上传 `docs/week3/rag-eval-knowledge.md`。
3. 查询文档详情或列表，确认文档已完成解析、切片和 embedding。
4. 依次调用用户端问答接口，例如 `chat/ask`。
5. 每条用例使用表格中的“测试问题”作为请求问题。
6. 记录响应中的 `answer`、`matched`、`retrievedChunkCount`、`rawRetrievedChunkCount`、`citations`、`conversationId`、`topK`。
7. 对需要追溯的用例，继续调用用户端问答详情接口，确认详情字段与 `chat/ask` 响应一致。
8. 对管理端追溯用例，调用管理端问答详情接口，确认 `userId`、`conversationId`、`topK`、`citations` 正确返回。
9. 对 LLM 不可用用例，临时关闭或错误配置 LLM 服务后再次提问，测试完成后恢复配置。
10. 将实际结果与本文件及 `rag-eval-cases.json` 的预期字段逐项对照。

## 6. 通过 / 不通过判断标准

### 通过标准

- 明确命中类问题 `matched=true`，且 `retrievedChunkCount` 满足最低要求。
- 需要 citations 的用例返回非空 citations，且包含可追溯字段。
- 答案包含预期关键词，且没有明显编造或与知识库相反的内容。
- 无命中和文档外问题 `matched=false`，并给出知识库范围内的拒答。
- 弱命中问题不应被强行回答为确定结论。
- AnswerExtractor 用例返回最终可展示文本，不暴露真实 LLM 原始结构。
- LLM 不可用时能明确降级，保留检索追溯信息，不伪装成正常生成成功。

### 不通过标准

- 无关问题出现 `matched=true` 或生成了脱离知识库的确定答案。
- 明确命中问题没有召回任何有效 chunk。
- citations 缺失、为空，或无法定位到文档和 chunk。
- `retrievedChunkCount` 与 `rawRetrievedChunkCount` 语义明显混乱。
- 返回上游 LLM 原始 JSON、数组、choices/message 等内部结构。
- LLM 不可用时抛出未处理异常，或把异常堆栈暴露给用户。

## 7. 后续自动化扩展方向

当前文件先作为人工验收基线。后续可在不改变用例语义的前提下扩展为自动化测试：

- 将 `rag-eval-cases.json` 作为测试数据源。
- 编写集成测试自动创建测试知识库、上传固定知识源、等待解析完成。
- 自动调用问答接口并断言 `matched`、最小召回数、citations 和关键词。
- 为 LLM 不可用场景增加 mock 或测试 profile。
- 将回归测试加入 RAG、Chat、Conversation、Document 生命周期相关改动的发布前检查。
