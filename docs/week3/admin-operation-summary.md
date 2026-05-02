# 管理端运营增强总结

## 1. 为什么需要管理端运营

知识库问答系统不能只关注“能否回答”，还需要知道：

- 哪些问题经常被问。
- 哪些问题没有命中。
- 用户对答案是否满意。
- 哪个知识库的命中率较低。

第三周的管理端增强不是完整运营后台，而是 MVP 阶段的基础运营分析能力，帮助后续补充知识库和排查 RAG 质量问题。

## 2. 管理端问答详情增强

接口：

- `GET /api/admin/chat/records`
- `GET /api/admin/chat/records/{id}`

数据来源：

- `chat_record`

展示信息：

- 问题、答案预览 / 完整答案。
- `answerStatus`
- `matched`
- `retrievedChunkCount`
- `rawRetrievedChunkCount`
- `topK`
- `citations`
- 创建时间。

运营价值：

- 管理员可以查看某次问答是否真的基于知识库回答。
- 可以通过 citations 排查引用是否合理。

## 3. 未命中问题治理基础版

接口：

- `GET /api/admin/chat/missed-questions`

数据来源：

- `chat_record.matched=false`

支持筛选：

- `knowledgeBaseId`
- `startTime`
- `endTime`
- `page`
- `size`

运营价值：

- 发现知识库缺口。
- 将高频未命中问题整理成后续补充文档。
- 评估 RAG 拒答策略是否过严或过松。

## 4. 反馈运营查看

接口：

- `GET /api/admin/chat/feedback`

数据来源：

- `chat_feedback`
- `chat_record`

支持筛选：

- `knowledgeBaseId`
- `rating=LIKE/DISLIKE`
- `startTime`
- `endTime`
- `page`
- `size`

运营价值：

- 快速查看用户不满意答案。
- 结合问答详情和 citations 判断是文档缺失、检索差还是 LLM 表达差。

## 5. 热门问题 TopN

接口：

- `GET /api/admin/chat/hot-questions`

数据来源：

- `chat_record.question`

支持筛选：

- `knowledgeBaseId`
- `startTime`
- `endTime`
- `limit`

运营价值：

- 找出用户最关心的问题。
- 为 README 示例、演示数据、知识库补充提供依据。

## 6. 基础统计接口

接口：

- `GET /api/admin/chat/stats`

数据来源：

- `chat_record`
- `chat_feedback`

统计指标：

- `totalChatCount`
- `matchedCount`
- `missedCount`
- `matchRate`
- `feedbackCount`
- `likeCount`
- `dislikeCount`

运营价值：

- 观察整体问答量和命中率。
- 粗略判断知识库质量。
- 为后续优化提供基础指标。

## 7. 每个功能对应的接口

| 功能 | 接口 | 数据来源 |
|---|---|---|
| 管理端问答列表 | `GET /api/admin/chat/records` | `chat_record` |
| 管理端问答详情 | `GET /api/admin/chat/records/{id}` | `chat_record.citations_json` |
| 未命中问题治理 | `GET /api/admin/chat/missed-questions` | `chat_record.matched=false` |
| 反馈运营查看 | `GET /api/admin/chat/feedback` | `chat_feedback` + `chat_record` |
| 热门问题 TopN | `GET /api/admin/chat/hot-questions` | `chat_record.question` |
| 基础统计 | `GET /api/admin/chat/stats` | `chat_record` + `chat_feedback` |

## 8. 对知识库运营的价值

这些接口形成了一个基础闭环：

1. 用户提问，系统记录命中状态和 citations。
2. 用户提交 LIKE / DISLIKE。
3. 管理端查看未命中和差评问题。
4. 运营者补充或修正文档。
5. 重新 process / embed。
6. 再通过问答和统计观察效果。

## 9. 当前边界

- 没有完整后台页面，当前是接口级能力。
- 没有自动生成补充文档。
- 没有复杂指标看板。
- 没有按用户、部门、标签等维度深度分析。
- 没有 A/B 测试或离线评估平台。

## 10. 后续可优化方向

- 将未命中问题支持标记处理状态。
- 增加按知识库的趋势统计。
- 增加差评原因分类。
- 支持从未命中问题一键生成待补充知识条目。
- 结合 RAG 评估集做定期回归。
