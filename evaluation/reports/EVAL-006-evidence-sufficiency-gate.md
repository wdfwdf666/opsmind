# EVAL-006：证据充足性门禁与低相关度拒答

## 目标

修复 EVAL-005 暴露的“检索只要返回 Top-K 就会生成答案”问题，在调用 Chat 模型前增加可解释的证据门禁，并扩大域外/相邻领域负样本覆盖。

## 数据版本

- `opsmind-answer-v1`：保留 EVAL-005 使用的 8 条历史基线，不修改原文件
- `opsmind-answer-v2`：20 条，其中 6 条可回答、14 条不可回答
- 新负样本覆盖人事、报销、采购、账号、VPN，以及 Redis、OOM、MySQL 恢复、Kubernetes、工单、扩容审批等容易与运维语料产生弱词面重合的问题

## 门禁规则

1. 精排无候选：`NO_EVIDENCE`。
2. Top-1 精排分数低于 `MINIMUM_EVIDENCE_SCORE`：`LOW_RELEVANCE`。
3. 只有通过门禁才组装 Parent 上下文并调用生成模型。
4. 响应返回 `topEvidenceScore` 和 `refusalReason`，评测逐用例保留同样字段。

首版本地阈值为 0.15。该值来自当前单文档、本地 Token Overlap Reranker 的探索性分布：

- 6 个正样本 Top-1：最低 0.179，最高 0.319
- 14 个负样本 Top-1：最低 0.018，最高 0.125

阈值位于当前最高负样本与最低正样本之间。它不是跨模型概率，不能直接视为 Qwen 或生产阈值。

## 验证环境

- 日期：2026-10-13
- PostgreSQL 17.11 + pgvector 0.8.7
- 文档 ID：`79fc1fcf-93f1-44a0-bc86-95cac599f082`
- Embedding：`local-feature-hashing-v1 / 256`
- Reranker：`local-token-overlap-reranker-v1`
- Generation：`local-extractive-answer-v1`
- 数据集：`opsmind-answer-v2`
- `topK=3`，`minimumEvidenceScore=0.15`

## 结果

| 指标 | EVAL-005 历史基线 | EVAL-006 |
|---|---:|---:|
| 用例数 | 8 | 20 |
| 可回答 / 不可回答 | 6 / 2 | 6 / 14 |
| Answer Correctness | 1.000 | 1.000 |
| Faithfulness Proxy | 1.000 | 1.000 |
| Citation Precision | 0.750 | 1.000 |
| Citation Recall | 1.000 | 1.000 |
| Refusal Precision | 0.000 | 1.000 |
| Refusal Recall | 0.000 | 1.000 |
| Refusal F1 | 0.000 | 1.000 |
| P50 | 2.53 ms | 2.57 ms |
| P95 | 40.29 ms | 4.75 ms |

6 个正样本全部继续回答，最低分样本“回滚完成后如何确认核心功能正常？”的 Top-1 为 0.179，高于阈值。14 个负样本全部以 `LOW_RELEVANCE` 拒答，不调用本地生成器；最高分负样本“Redis 内存使用率过高怎么处理？”为 0.125，低于阈值。

两列使用的数据集规模不同，只用于展示工程演进，不能当作严格的同测试集模型对照。v2 同时用于探索分数和验证门禁，1.000 存在明显的过拟合风险。

## 修改效果

- 域外问题不再因为向量检索“总能返回结果”而自动进入生成阶段。
- 拒答原因和门禁分数可解释、可调试、可进入评测报告。
- 低相关度请求不会产生 Chat Token 和生成费用。
- v1 不被覆盖，历史报告仍可复现；v2 成为新的默认评测数据集。

## 风险与下一步

1. 新增更多文档、同主题困难负样本和语义改写，按校准集/测试集拆分后重新选阈值。
2. 分别为每个 Reranker 模型版本维护阈值，不允许换模型后沿用旧值。
3. 在获得数据外发授权后采集 Qwen Reranker 分数分布，当前 0.15 只作为本地开发默认值。
4. 监控拒答率、低相关分数分布和人工纠错，防止知识库增长后阈值漂移。
