# EVAL-005：带引用回答质量与拒答基线

## 目标

将 FEAT-013 的单条工程验证升级为版本化自动评测，持续观察事实覆盖、引用质量、拒答效果、延迟和 Token 成本，并用不可回答问题暴露幻觉风险。

## 数据集与指标口径

- 数据集：`opsmind-answer-v1`
- 语料：`opsmind-operations.md`
- 用例：8 条，其中 6 条可回答、2 条不可回答
- 可回答样本标注：问题、期望事实组、期望标题路径
- 不可回答样本标注：`answerable=false`，期望事实和来源均为空
- Answer Correctness：回答命中的期望事实组比例；同一事实组允许多个等价短语
- Citation Precision/Recall：实际引用标题路径与期望标题路径的微平均精确率/召回率
- Refusal Precision/Recall/F1：以不可回答样本为正类计算
- Faithfulness Proxy：去除 `[S数字]` 后，回答字符 bigram 被实际引用摘录覆盖的比例
- 延迟：8 次顺序请求的 nearest-rank P50/P95

`faithfulnessProxy` 是无外部 Judge、可重复执行的词面回归信号，不是语义忠实度结论。它不能可靠识别同义改写、否定、数字冲突或错误推理。

## 验证环境

- 日期：2026-10-13
- PostgreSQL 17.11 + pgvector 0.8.7
- 文档 ID：`79fc1fcf-93f1-44a0-bc86-95cac599f082`
- Embedding：`local-feature-hashing-v1 / 256`
- Reranker：`local-token-overlap-reranker-v1`
- Generation：`local-extractive-answer-v1`
- `topK=3`

## 结果

| 指标 | 结果 |
|---|---:|
| Answer Correctness | 1.000 |
| Faithfulness Proxy | 1.000 |
| Citation Precision | 0.750 |
| Citation Recall | 1.000 |
| Refusal Precision | 0.000 |
| Refusal Recall | 0.000 |
| Refusal F1 | 0.000 |
| P50 | 2.53 ms |
| P95 | 40.29 ms |
| Token | 0（本地确定性生成器） |

6 条可回答用例均命中全部期望事实和正确标题路径。本地生成器直接抽取证据，所以其 Faithfulness Proxy 为 1.000；这不能外推到 Qwen 生成质量。

两个不可回答问题“Kubernetes 集群证书轮换”和“员工出差住宿标准”都没有被拒答，反而错误引用“恢复验证”章节。它们使 Citation Precision 降至 0.750，且 Refusal Precision/Recall/F1 均为 0。这说明“检索始终返回 Top-K + 只有零候选才拒答”的策略无法识别域外问题。

## 修改效果

- 将回答回归从 1 条人工观察扩展为 8 条版本化用例。
- 每次 Prompt、模型、检索、分块或拒答策略修改后，可以同口径比较逐用例结果。
- 失败不再停留在“感觉可能幻觉”，而是定位为 2 个明确域外样本和 0.000 的拒答 F1。
- API 同时返回检索/生成模型、逐用例答案、引用标题、事实命中、延迟和 Token，可供 CI 保存为报告。

## 下一步

1. 扩充至少 30 条域外、相邻领域和困难负样本，避免用两个样本直接调阈值。
2. 分别记录 Reranker Top-1 分数、Top-1/Top-2 margin、关键词命中和证据覆盖，寻找可解释的拒答特征。
3. 在验证集上选择阈值，在独立测试集报告拒答 Precision/Recall/F1，防止过拟合。
4. 增加独立 Qwen Judge 或人工抽检评测语义 Faithfulness；Judge Prompt、模型版本和温度必须固定并记录。
5. 真实 Qwen 评测需获得数据外发授权，并单独记录费用、Token 与远程 P95。
