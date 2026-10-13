# EVAL-008：本地降级 Planner v2 开发集回归

## 目标

根据 EVAL-007 的三个失败案例，改进本地确定性 Planner 对否定句、流程咨询和间接写意图的识别，并使用同一数据集做前后对照。

## 修改

`local-intent-router-v2` 将路由顺序调整为：

1. 识别“不要、无需、不需要、暂不、先不”等对写操作的否定。
2. 识别“如何创建、创建前需要哪些审批、创建流程”等知识咨询。
3. 识别明确写意图，并扩展“登记、建立、开一张、建一个”等表达。
4. 其他输入默认进入 `KNOWLEDGE_QA`。

输入先经过 NFKC、大小写和空白归一化。每个分支返回不同的 `plannerReason`，便于审计命中原因。

## 环境与口径

- 数据集：`opsmind-agent-routing-v1`，24 条
- Provider：local
- 对照模型：`local-intent-router-v1`、`local-intent-router-v2`
- Java 21 / Spring Boot 4.1.1 / PostgreSQL 17.11
- 日期：2026-10-13

这是在已观察失败的同一开发回归集上进行的优化，不是独立测试集，也不代表 Qwen 或生产准确率。

## 结果

| 指标 | v1 | v2 | 变化 |
|---|---:|---:|---:|
| Correct | 21/24 | 24/24 | +3 |
| Accuracy | 0.8750 | 1.0000 | +0.1250 |
| Macro Precision | 0.8776 | 1.0000 | +0.1224 |
| Macro Recall | 0.8750 | 1.0000 | +0.1250 |
| Macro-F1 | 0.8748 | 1.0000 | +0.1252 |
| Dangerous Write Misroute Rate | 0.1667 | 0 | -0.1667 |
| P50 | 0.0012 ms | 0.0143 ms | +0.0131 ms |
| P95 | 0.1301 ms | 0.1512 ms | +0.0211 ms |

v2 修复了：

- `qa-negation-01`：否定写操作，改为 `KNOWLEDGE_QA`。
- `qa-workflow-01`：流程/审批咨询，改为 `KNOWLEDGE_QA`。
- `ticket-indirect-01`：识别“登记工单”，改为 `CREATE_TICKET`。

没有出现新的开发集失败。增加的正则判断使 P50/P95 略升，但绝对耗时仍低于 0.2 ms，当前差异没有业务意义。

## 结论与下一步

评测驱动修改已消除已知失败，并保持服务端审批门禁不变。下一步必须冻结未参与本轮规则设计的独立测试集，验证否定范围、反问、混合意图、同义词和 Prompt Injection 的泛化；独立结果出来前不把 1.0 写作最终准确率。
