# VERIFY-003：写工具与 Human-in-the-loop 审批验证

## 目标

验证 `知识检索 → 工单草稿 → 等待审批 → 批准/拒绝 → 写工具 → 持久化审计` 的闭环，重点证明审批前没有副作用、批准具有幂等边界、拒绝不会创建工单。

## 环境与口径

- 日期：2026-10-13
- 数据库：PostgreSQL 17.11 + pgvector 0.8.7
- Provider：本地 256 维特征哈希 Embedding、本地 Token Overlap Reranker、本地确定性抽取生成器
- 接口：`POST /api/v1/agent/runs`、`POST /api/v1/agent/runs/{runId}/approval`、`GET /api/v1/agent/runs/{runId}`
- 任务类型：`CREATE_TICKET`
- 写工具：`create_ticket`，风险 `WRITE_REQUIRES_APPROVAL`
- 外部系统：工单保存在项目内 `support_ticket` 模拟表，不调用外部工单平台

## 批准链路

创建草稿请求：

```json
{
  "query": "数据库连接池持续告警，请创建 P2 工单",
  "taskType": "CREATE_TICKET"
}
```

审批前结果：

| 项目 | 结果 |
|---|---|
| Run 状态 | `WAITING_APPROVAL` |
| Approval 状态 | `PENDING` |
| 工具风险 | `WRITE_REQUIRES_APPROVAL` |
| 审计步骤 | 4 |
| `support_ticket` 新增数 | 0 |

步骤为 `RECEIVED → PLANNED → TOOL_CALL(knowledge_search) → APPROVAL_REQUESTED(create_ticket)`。响应中的 `approval.draft` 包含标题、用户描述、知识库建议和 P2 优先级。

批准请求：

```json
{
  "decision": "APPROVE",
  "reason": "已确认影响范围，同意创建"
}
```

批准后结果：

| 项目 | 结果 |
|---|---|
| Run 状态 | `COMPLETED` |
| Approval 状态 | `APPROVED` |
| Ticket 状态 | `OPEN` |
| 审计步骤 | 7 |
| 该 Run 的数据库工单数 | 1 |

新增步骤为 `APPROVED → TOOL_CALL(create_ticket) → RESPONDED`。随后以相同决定再次调用审批接口，返回的 Ticket ID 与首次完全相同，数据库工单数仍为 1，审计步骤仍为 7。

## 拒绝链路

对第二个 `CREATE_TICKET` Run 提交：

```json
{
  "decision": "REJECT",
  "reason": "先继续观察，不创建工单"
}
```

结果：Run 为 `REJECTED`，Approval 为 `REJECTED`，步骤数为 5，响应 `ticket=null`，数据库中该 Run 的工单数为 0。

## 一致性与幂等边界

1. 草稿审批记录、Run 的 `WAITING_APPROVAL` 状态和 `APPROVAL_REQUESTED` Step 在同一个仓储事务中提交。
2. 批准决定、工单 INSERT、3 个后续 Step 和 Run 完成状态在同一个 Spring 事务中提交。
3. `agent_approval.run_id` 唯一，保证每个 Run 只有一个审批对象。
4. `support_ticket.agent_run_id` 唯一，作为数据库最终幂等约束。
5. 相同决定的顺序重试返回已有结果；相反决定返回冲突。

## 自动化验证

新增或扩展 6 个测试，覆盖草稿无副作用、批准、批准重试、拒绝、审批 JDBC 回读和真实 HTTP 流程；OpenAPI 测试增加审批路径断言。

完整测试结果：71 个测试，0 失败，0 错误；Maven 打包成功。

## 结论与边界

FEAT-017 已证明 Agent 写操作受到真实审批门禁和数据库唯一约束保护，不是让模型直接触发副作用。当前审批尚未绑定认证用户、角色或租户，也没有审批过期和跨系统可靠投递；本地 `support_ticket` 是演示适配器。下一阶段增加审批人身份/RBAC、租约、Outbox 和外部工单适配器，再引入受白名单和 Schema 约束的 Qwen Planner。
