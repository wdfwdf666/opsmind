# VERIFY-002：只读 Agent 工具与持久化审计验证

## 目标

验证 `Agent Run → 风险策略 → knowledge_search → RAG 回答 → 审计回读` 的最小闭环，并确认实际问答省略 `documentId` 时会自动跨文档选择证据，而不是依赖用户手工选文档。

## 环境与口径

- 日期：2026-10-13
- 数据库：PostgreSQL 17.11 + pgvector 0.8.7
- 数据状态：5 条文档记录、3 个 Child Chunk、6 条向量记录
- Provider：本地 256 维特征哈希 Embedding、本地 Token Overlap Reranker、本地确定性抽取生成器
- 接口：`POST /api/v1/agent/runs`、`GET /api/v1/agent/runs/{runId}`
- 文档范围：请求不传 `documentId`，由检索链路自动选择
- 外部模型：本次未调用远程 Qwen，不产生外部 Token 或费用

## 自动选文档用例

请求：

```json
{
  "query": "数据库连接池告警应该检查什么？"
}
```

结果：

- Run 状态：`COMPLETED`
- `documentId` 请求范围：`null`，表示全部可检索文档
- 自动命中文档：`feat007.md`
- 自动命中标题：`OpsMind 运维手册 > 告警处置`
- Top-1 精排分：`0.28968272975116055`
- 回答：包含合法引用 `[S1]`
- 引用：1 条，保留文档、Parent/Child Chunk、标题、摘录和分数
- 审计步骤：4 条，序号严格为 1、2、3、4

审计轨迹：

| 序号 | 类型 | 工具 | 风险 | 结果 | 关键元数据 |
|---:|---|---|---|---|---|
| 1 | `RECEIVED` | - | - | 成功 | 查询字符数、`ALL_DOCUMENTS` |
| 2 | `PLANNED` | `knowledge_search` | `READ_ONLY` | 成功 | `DETERMINISTIC_SINGLE_TOOL` |
| 3 | `TOOL_CALL` | `knowledge_search` | `READ_ONLY` | 成功 | 拒答状态、引用数、Top-1 分数 |
| 4 | `RESPONDED` | - | - | 成功 | 回答字符数、Token 数 |

随后使用返回的 Run ID 调用 GET，读回的运行字段、引用和 4 个步骤与 POST 响应一致，证明结果来自持久化审计仓储。

## 拒答用例

请求：

```json
{
  "query": "员工出差报销标准是多少？"
}
```

结果：Run 状态为 `COMPLETED`，`refused=true`，`refusalReason=LOW_RELEVANCE`，Top-1=`0.03951303898468`，步骤数仍为 4。这里 `COMPLETED` 表示 Agent 工具链按设计完成，拒答是成功的业务结果，不是运行故障。

两次请求后数据库包含 2 个 `COMPLETED` Run 和 8 个 Step。

## 自动化验证

新增 5 个测试覆盖：

1. 只读工具执行后产生完整回答与 1~4 有序步骤。
2. 需要审批的写风险工具在执行前被策略拦截，并保存失败 Run 与失败 Tool Step。
3. 空问题在创建审计 Run 前被拒绝。
4. JDBC 完成 Run 可回读回答、引用和步骤。
5. JDBC 失败 Run 只保存脱敏错误，并验证两个 Agent OpenAPI 路径。

完整测试结果：65 个测试，0 失败，0 错误；Maven 打包成功。

## 结论与边界

FEAT-016 已建立确定性、可回放的只读 Agent 最小闭环，并证明正常问答不需要用户选择文档。当前验证的是单工具工程编排与审计正确性，不代表已经实现自主多工具规划。下一步增加工单草稿写工具、持久化审批状态、审批幂等键和恢复流程，再评估接入 Qwen 结构化 Planner。
