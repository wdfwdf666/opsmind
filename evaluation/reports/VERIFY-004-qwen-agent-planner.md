# VERIFY-004：Qwen 结构化 Planner 与受控路由验证

## 验证目标

验证省略 `taskType` 时 Planner 能在服务端白名单内选择知识问答或创建工单，并保证模型路由不会绕过写工具审批；同时验证显式覆盖与路由元数据审计。

## 环境

- 日期：2026-10-13
- Java 21 / Spring Boot 4.1.1
- PostgreSQL 17.11 + pgvector 0.8.7，端口 5433
- 真实接口：local Planner、Embedding、Reranker、Generation
- Qwen 协议：`MockRestServiceServer` 模拟 OpenAI-compatible Chat Completions

真实接口使用本地 Provider 验证路由、状态机、审批和持久化，不代表 Qwen 语义准确率。Qwen 请求格式、鉴权、严格 JSON 和错误映射由契约测试覆盖。

## 修改前问题

- `taskType` 为空时固定选择 `KNOWLEDGE_QA`。
- 调用方必须理解任务枚举才能创建工单。
- Run 没有路由来源、模型和理由。

## 验证结果

完整执行 `.\mvnw.cmd test`：78 个测试通过，0 失败，0 错误。首次执行发现 H2 不支持单条 `ALTER TABLE` 连续添加多列；将 V8 拆为四条语句后，H2 与 PostgreSQL 均迁移成功。

| 场景 | 是否带 taskType | taskType | 来源 | 状态/效果 |
|---|---:|---|---|---|
| 数据库连接池告警检查 | 否 | `KNOWLEDGE_QA` | `PLANNER` | `COMPLETED` |
| 明确要求创建 P2 工单 | 否 | `CREATE_TICKET` | `PLANNER` | `WAITING_APPROVAL` |
| 拒绝验证工单 | - | `CREATE_TICKET` | `PLANNER` | `REJECTED`，无 Ticket |
| 创建工单语句但显式覆盖 | 是 | `KNOWLEDGE_QA` | `EXPLICIT` | `COMPLETED` |

健康检查为 `UP`，真实 PostgreSQL 从 V7 升至 V8。临时应用已关闭。

## 修改效果

- 修改前：0 个 Planner；省略任务类型时固定知识问答；71 个测试。
- 修改后：Qwen/local 两个 Planner；两种受控任务、两种来源、四个持久化路由字段；78 个测试。
- Planner 选择创建工单后仍先进入审批，拒绝验证产生 0 张工单。

## 风险与后续

尚未用独立标注集测量 Qwen 路由准确率；Planner 失败发生在创建 Run 之前。下一步建立包含普通问答、隐式动作、否定句、Prompt Injection 和歧义请求的路由评测集，计算 Accuracy、Macro-F1 和危险误路由率，再增加超时、重试与本地降级。
