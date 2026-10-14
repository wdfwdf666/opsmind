# VERIFY-007：Prometheus、Grafana 与 Alertmanager 监控闭环验证

## 目标

验证 Spring Boot 保持宿主机运行时，Prometheus 容器能够抓取 Planner 指标，Grafana 自动加载数据源与 Dashboard，Prometheus 告警能够进入 Alertmanager，并在 Planner 恢复后清除熔断告警。

## 环境

- Java 21 / Spring Boot 4.1.1
- PostgreSQL 17.11 + pgvector 0.8.7
- Prometheus 3.15.0
- Grafana 12.4.12
- Alertmanager 0.30.0
- Windows Docker Desktop
- 应用验证端口：18080（保留用户现有 8080 IntelliJ 进程）

## 静态与自动化验证

| 检查 | 结果 |
|---|---|
| `docker compose -p opsmind config` | 通过 |
| `promtool check config /etc/prometheus/prometheus.yml` | 通过，发现 1 个规则文件、4 条规则 |
| `promtool check rules /etc/prometheus/alerts.yml` | 通过，4 条规则 |
| `amtool check-config /etc/alertmanager/alertmanager.yml` | 通过，1 个接收器 |
| `.\mvnw.cmd test` | 92/92 通过 |
| `.\mvnw.cmd package -DskipTests` | BUILD SUCCESS |
| Dashboard JSON 解析 | 通过，UID=`opsmind-agent`，刷新周期 10 秒 |

新增的 `PrometheusEndpointTests` 验证 `/actuator/prometheus` 返回 Prometheus 文本格式，并包含 `opsmind_agent_planner_circuit_state`。生产与测试配置均显式启用 Exporter，避免 Spring Boot 测试环境默认关闭 Metrics Export。

## 真实服务验证

1. 启动 Alertmanager、Prometheus、Grafana，并让 Prometheus 临时抓取 `host.docker.internal:18080/actuator/prometheus`。
2. Prometheus Target 返回 `health=up`，无抓取错误。
3. Grafana `/api/health` 返回数据库 `ok`、版本 `12.4.12`；Prometheus 数据源健康检查返回 `Successfully queried the Prometheus API.`。
4. Grafana API 成功读取 UID 为 `opsmind-agent`、标题为 `OpsMind Agent Observability` 的预置 Dashboard。
5. Alertmanager `/-/ready` 返回 HTTP 200。

## 故障注入

临时应用使用以下关键参数：

- Planner Provider：Qwen
- Qwen 地址：本机未监听端口
- 最大远程尝试：1
- 熔断失败阈值：2
- 熔断开放窗口：300 秒
- Embedding、Rerank、Generation：local

首批连续发起 12 次 Agent 请求，HTTP 状态全部为 200，均由本地 Planner 保守降级完成路由。应用指标为：

| 指标 | 值 |
|---|---:|
| 远程 I/O 失败尝试 | 2 |
| 熔断打开次数 | 1 |
| 熔断旁路次数 | 10 |
| 熔断状态 | 1（OPEN） |
| Planner 降级请求 | 12 |

为了确保 Prometheus 在两个抓取点之间观察到 Counter 增量，随后再发起 12 次请求；5 分钟窗口查询得到约 12.30 次请求增量和约 12.30 次降级增量。由于 Prometheus 对区间边界插值，`increase()` 结果可以是小数。

等待规则持续时间后：

- `OpsMindPlannerCircuitOpen`：Prometheus `firing`，Alertmanager `active`。
- `OpsMindPlannerFallbackRateHigh`：Prometheus `firing`，Alertmanager `active`。
- `OpsMindPlannerP95LatencyHigh`：未触发，故障旁路延迟低于 2 秒，符合预期。

## 恢复验证

启动本地 OpenAI Chat Completions 兼容响应模拟器，返回合法的 Planner JSON。下一次探测请求返回 `plannerProvider=qwen`、`plannerReason=validation recovered`，熔断状态 Gauge 从 1 变为 0；随后 Prometheus 活动告警中不再包含 `OpsMindPlannerCircuitOpen`。高降级率规则使用 5 分钟滚动窗口，因此会在历史降级样本退出窗口后自然恢复，而不是在单次成功请求后立即清除。

## 清理与结论

验证结束后关闭了 18080 临时应用、响应模拟器以及 Grafana、Prometheus、Alertmanager 容器，保留验证前已经运行的 PostgreSQL 和用户 IntelliJ 8080 进程。

结论：抓取、存储、查询、看板 Provisioning、告警计算、Alertmanager 投递和熔断恢复链路均已验证。当前接收器只用于本地 UI；生产前还需保护 Actuator、修改管理员密码、接入真实通知渠道并校准阈值。
