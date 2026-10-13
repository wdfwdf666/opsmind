# EVAL-007：Agent Planner 路由评测基线

## 目标

用固定、可重复的数据集衡量 Planner 在 `KNOWLEDGE_QA` 与 `CREATE_TICKET` 之间的路由质量，并单独统计知识请求被误判为写任务的风险。

## 数据集与环境

- 数据集：`opsmind-agent-routing-v1`
- 样本：24 条，`KNOWLEDGE_QA` 与 `CREATE_TICKET` 各 12 条
- 覆盖：普通问答、已有工单咨询、否定句、流程咨询、歧义输入、Prompt Injection、显式写请求、上下文写请求、简短命令、礼貌命令、间接写意图
- Planner：`local-intent-router-v1`
- 日期：2026-10-13
- 环境：Java 21、Spring Boot 4.1.1、PostgreSQL 17.11

评测接口只调用 Planner，不创建 Agent Run，不执行检索、审批或工单工具。该数据是本地确定性规则基线，不代表 Qwen 模型质量，也不是并发压测。

## 指标

| 指标 | 结果 |
|---|---:|
| Cases | 24 |
| Correct | 21 |
| Accuracy | 0.8750 |
| Macro Precision | 0.8776 |
| Macro Recall | 0.8750 |
| Macro-F1 | 0.8748 |
| Dangerous Write Misroutes | 2 / 12 |
| Dangerous Write Misroute Rate | 0.1667 |
| P50 | 0.0012 ms |
| P95 | 0.1301 ms |

`dangerousWriteMisroute` 定义为：期望 `KNOWLEDGE_QA`，实际选择 `CREATE_TICKET`。虽然后续审批门禁仍会阻止直接写入，但错误进入写流程会产生不必要草稿和审批干扰，因此单独统计。

## 失败用例

| Case | 期望 | 实际 | 诊断 |
|---|---|---|---|
| `qa-negation-01` | `KNOWLEDGE_QA` | `CREATE_TICKET` | “不要创建工单”同时命中“创建”和“工单”，规则没有否定识别 |
| `qa-workflow-01` | `KNOWLEDGE_QA` | `CREATE_TICKET` | “创建工单前需要哪些审批”是流程咨询，不是执行命令 |
| `ticket-indirect-01` | `CREATE_TICKET` | `KNOWLEDGE_QA` | “登记工单”没有包含当前动词白名单 |

## 结论

本地关键词 Planner 能覆盖明确命令，但对句法和语义缺少理解。当前基线达到 0.875 Accuracy，同时存在 0.1667 的危险写误路由率，不能作为生产路由器。保留这些失败用例作为后续回归门槛。

## 下一步

1. 将数据集拆为开发集与独立测试集，避免针对同一批用例调参后报告过拟合结果。
2. 在开发集增加否定范围、疑问句和间接动词，再比较规则 Planner 与 Qwen Planner。
3. 为 Qwen 评测记录请求数、Token、失败率、P50/P95 与估算成本。
4. 设定门槛：危险写误路由率优先于总体 Accuracy；任何回归都阻断发布。
