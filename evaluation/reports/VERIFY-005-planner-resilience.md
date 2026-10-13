# VERIFY-005：Qwen Planner 超时、重试与本地降级验证

## 验证目标

验证 Qwen Planner 在限流、上游服务异常和网络不可达时按策略重试并降级，同时确认普通 4xx 不重复请求、降级来源可持久化审计，且本地规则不会把否定写请求错误路由到写任务。

## 环境

- 日期：2026-10-13
- Java 21 / Spring Boot 4.1.1
- PostgreSQL 17.11 + pgvector 0.8.7，端口 5433
- 真实故障注入：Planner Base URL 指向本机未监听端口
- Planner 配置：连接超时 200 ms、读取超时 300 ms、最多 2 次尝试、本地降级开启
- Embedding、Reranker、Generation：local Provider

故障注入使用占位 API Key，不读取或输出真实密钥。临时应用监听 18080，验证完成后关闭。

## 修改前问题

- Qwen Planner 没有独立连接与读取超时预算。
- 网络错误、429 和 5xx 第一次失败即中断 Agent。
- 没有可审计的本地恢复路径。

## 自动化验证

完整执行 `.\mvnw.cmd test`：87 个测试通过，0 失败，0 错误。

| 场景 | 远程调用次数 | 结果 |
|---|---:|---|
| 第一次 HTTP 429，第二次返回合法 JSON | 2 | 使用 Qwen 决策，Provider=`qwen` |
| 连续两次 HTTP 500 | 2 | 降级到 `local-intent-router-v2` |
| HTTP 401，最大尝试配置为 3 | 1 | 不重试，立即本地降级 |
| 否定写请求“不要创建工单” | 2 次 500 后降级 | `KNOWLEDGE_QA`，保持保守路由 |
| 关闭降级后的 500/非法响应 | 1 | 返回既有脱敏 Planner 错误 |

首次完整测试暴露配置 record 多构造器导致 Spring Boot 绑定失败；移除兼容重载构造器、统一显式测试配置后，应用上下文与全部回归恢复通过。

## 真实故障注入结果

向 `POST /api/v1/agent/runs` 提交知识问答请求，Qwen Planner 指向不可达端口：

| 指标 | 结果 |
|---|---|
| 端到端耗时 | 438 ms |
| Run 状态 | `COMPLETED` |
| 任务类型/来源 | `KNOWLEDGE_QA` / `PLANNER` |
| Planner Provider | `local-fallback` |
| Planner 模型 | `local-intent-router-v2` |
| 失败审计 | `AGENT_PLANNER_PROVIDER_UNAVAILABLE`, `attempts=2` |
| 审计步骤数 | 4 |

随后通过 `GET /api/v1/agent/runs/{runId}` 回读，Provider、模型和理由与首次响应一致，证明降级不是只存在于内存响应中。该验证产生一个已完成的只读 Agent Run，没有执行写工具。

## 修改效果

- 修改前：一次远程故障直接失败，缺少超时预算和恢复审计，84 个测试。
- 修改后：连接/读取双超时、可配置 1～3 次尝试、按错误类型选择重试、local v2 降级并持久化来源，87 个测试。
- 写操作边界不变：Planner 只选择白名单任务，`CREATE_TICKET` 仍必须进入人工审批。

## 风险与后续

当前是应用线程内的固定次数立即重试，尚未加入指数退避、抖动、熔断、并发隔离和 Micrometer 指标。配置缺失刻意在重试前返回明确错误；若关闭降级或本地 Planner 失败，Run 仍不会创建。下一步增加 Resilience4j/Micrometer，并以独立冻结数据集分别评估 Qwen 与 local fallback。
