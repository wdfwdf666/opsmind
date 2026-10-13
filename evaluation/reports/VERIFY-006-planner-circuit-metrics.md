# VERIFY-006：Planner 指数退避、熔断与指标验证

## 验证目标

验证 Qwen Planner 的重试延迟按指数增长，持续故障达到阈值后熔断，开放状态直接旁路到本地 Planner，并通过 Actuator 暴露低基数 Micrometer 指标。

## 环境

- 日期：2026-10-13
- Java 21 / Spring Boot 4.1.1
- PostgreSQL 17.11 + pgvector 0.8.7，端口 5433
- 临时应用端口：18080
- Qwen Planner：指向本机未监听端口，使用占位 Key
- 最大尝试：1；熔断失败阈值：2；开放时长：30 秒
- Embedding、Reranker、Generation：local Provider

验证完成后已关闭临时应用。该过程新增三个只读、已完成 Agent Run，没有执行写工具。

## 修改前问题

- 重试之间没有退避，会立即再次请求故障上游。
- 每个新请求都会重新连接持续故障的 Planner。
- 缺少 Planner 专用成功率、延迟、重试、降级和熔断指标。

## 自动化验证

完整执行 `.\mvnw.cmd test`：91 个测试通过，0 失败，0 错误。

| 场景 | 验证结果 |
|---|---|
| 两次 500 后第三次成功 | 注入延迟为 25、50 ms，符合指数增长 |
| 连续两次最终失败 | 状态从 `CLOSED` 进入 `OPEN` |
| 开放状态第三次请求 | 远程调用次数不增加，原因记录 `CIRCUIT_OPEN/attempts=0` |
| 开放窗口到期 | 仅一个调用获得 `HALF_OPEN` 探测权限 |
| 半开探测成功 | 状态恢复 `CLOSED`，失败计数清零 |
| 退避等待被中断 | 恢复线程中断标记并终止规划，不继续执行下游 |
| Micrometer | Counter、Timer、Gauge 的值与调用次数一致 |

首次定向测试使用了缺少内层转义的双层 JSON Fixture，严格解析器将其识别为非法响应并执行降级；修正 Fixture 后 13 个 Planner 定向测试全部通过。

## 真实故障注入结果

| 请求 | 端到端耗时 | 远程尝试 | Planner 结果 | 熔断行为 |
|---:|---:|---:|---|---|
| 1 | 323 ms | 1 | `local-fallback` | 失败计数 1 |
| 2 | 27 ms | 1 | `local-fallback` | 达到阈值，打开熔断 |
| 3 | 23 ms | 0 | `local-fallback` | `OPEN` 状态直接旁路 |

第三次 Run 的审计原因为：`AGENT_PLANNER_CIRCUIT_OPEN, attempts=0`。三个 Run 状态均为 `COMPLETED`，模型均为 `local-intent-router-v2`。

Actuator 指标快照：

| 指标 | 值 |
|---|---:|
| `opsmind.agent.planner.attempts` | 2 |
| `opsmind.agent.planner.requests` | 3 |
| `opsmind.agent.planner.fallbacks` | 3 |
| `opsmind.agent.planner.circuit.opened` | 1 |
| `opsmind.agent.planner.circuit.rejected` | 1 |
| `opsmind.agent.planner.circuit.state` | 1（OPEN） |
| `opsmind.agent.planner.duration` | count=3 |

## 修改效果

- 修改前：持续故障时每个请求都访问远程服务，无 Planner 指标，87 个测试。
- 修改后：指数退避、三态熔断、开放状态快速旁路、8 类指标，91 个测试。
- 本次样本中，熔断旁路请求较首次故障请求从 323 ms 降至 23 ms；该数据用于证明旁路生效，不作为生产性能结论。

## 风险与后续

熔断状态只在当前 JVM 内生效，多实例之间不会共享；退避尚无随机抖动；Actuator 已提供原始指标，但尚未配置 Prometheus 告警和 Grafana 仪表盘。后续应增加降级率、熔断打开和 P95 延迟告警，并通过压测校准阈值。
