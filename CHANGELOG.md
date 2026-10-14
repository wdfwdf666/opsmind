# 变更记录

本文件记录项目中所有可交付的功能实现、缺陷修复、性能优化、架构调整和重要配置变更。记录采用倒序排列，最新变更置顶。

## [DOC-002] 增加面向零基础读者的源码内注释

- **日期**：2026-10-15
- **类型**：文档 / 源码注释 / Spring Boot / 可维护性
- **状态**：已完成
- **关联需求或问题**：完整学习指南解决了项目全貌问题，但初学者回到 IDE 阅读源码时，仍难以判断 Spring 注解何时生效、各层为何分开、关键流程为什么这样编排。
- **问题**：原生产源码几乎没有 Javadoc；Controller、Service、Repository、配置绑定、条件 Bean、事务、重试、熔断、父子分块、RRF 和 RAG 引用校验等意图需要反复对照外部文档。
- **修改内容**：为全部 21 个生产包增加 `package-info.java`，在包内解释 api/application/domain/infrastructure/config 分层职责；为启动类、统一响应、异常处理、文档接口、上传解析、父子分块、混合检索、重排、RAG、Qwen 调用、Agent 状态机、审批策略、Planner 重试降级、熔断、JDBC、离线评测和系统示例增加类级、方法级及关键语句注释；同时为 `application.yml` 与 `compose.yml` 增加配置和容器通信说明。
- **修改效果**：初学者可直接在 IDE 中从注解、依赖注入和请求映射开始阅读，并沿文档入库、检索问答、Agent 审批和监控链路理解代码；注释重点说明设计原因和边界，而不是逐行复述 Java 语法。
- **影响范围**：仅增加注释和包文档，不改变业务接口、数据库结构、配置默认值或运行逻辑。
- **验证方式**：执行完整 Maven 测试和跳过测试的打包，确认 package-info 与 Javadoc 不影响编译和现有行为。

## [DOC-001] 增加面向 Java 初学者的完整项目学习指南

- **日期**：2026-10-14
- **类型**：文档 / 架构 / Spring Boot / 学习指南
- **状态**：已完成
- **关联需求或问题**：项目已覆盖文档入库、RAG、Agent、评测和监控，但 README 更偏向运行状态和接口速查；只有少量 Java 基础、尚不熟悉 Spring Boot 的读者难以理解一次请求怎样跨越各层，以及代码为何使用依赖注入、接口、事务和条件 Bean。
- **问题**：现有信息分散在源码、README、迁移脚本和验证报告中，缺少从 Java/Spring Boot 基础出发的统一学习路径，也缺少对企业化演进方向的系统说明。
- **修改内容**：新增 `docs/OpsMind项目完整学习指南.md`，从 Java、Spring、Spring Boot 的关系开始，说明运行架构、Maven、分层、常用注解、文档入库、Parent-Child 分块、Embedding、混合检索、Rerank、RAG 门禁与引用、Planner、Tool Policy、审批状态机、数据库、Flyway、评测、监控、测试、排障、企业演进和面试表达；README 增加入口链接。
- **修改效果**：读者可以先建立整体链路，再按 Controller → Service → Repository → Infrastructure 的顺序阅读代码，并把当前原型与企业生产实现的差距对应起来。
- **影响文件**：完整学习指南、README、CHANGELOG。

## [FEAT-023] 建立 Prometheus、Grafana 与 Alertmanager 监控闭环

- **日期**：2026-10-14
- **类型**：增强 / 可观测性 / 监控 / 告警
- **状态**：已完成；自动化测试、配置检查与真实故障注入通过
- **关联需求或问题**：继续完善项目第 24 步；FEAT-022 已发布 Planner Micrometer 指标，但只能逐个访问 Actuator 查看，没有时序存储、统一看板、告警计算、聚合与静默入口。
- **问题**：运行人员无法持续观察请求速率、Qwen/降级占比、P95、重试和熔断状态；应用离线、持续降级与高延迟也不会主动进入告警状态。仅有瞬时 Actuator 数值不足以定位时间窗口内的异常。
- **修改内容**：引入 `micrometer-registry-prometheus`，显式启用 Prometheus Exporter 并开放 `/actuator/prometheus`；为 Planner Timer 启用直方图和 100 ms、500 ms、1 s、2 s、5 s SLO 桶。Compose 增加 Prometheus 3.15.0、Grafana 12.4.12、Alertmanager 0.30.0、7 天保留期、项目内绑定挂载和 `host-gateway`。新增自动 Provisioning 的 Prometheus 数据源、固定 UID `opsmind-agent` Dashboard、Alertmanager `local-ui` 接收器和四条告警规则。新增端点契约测试、Grafana 环境变量模板、README 与 VERIFY-007 报告。
- **实现中发现的问题**：Spring Boot 4 的 MockMvc 自动配置注解位于 `org.springframework.boot.webmvc.test.autoconfigure`，首次沿用旧包名导致测试编译失败；测试资源的 `application.yml` 会覆盖主配置，首次只改主配置导致端点测试返回 404。补齐测试配置后又发现 Spring Boot 测试默认关闭 Metrics Exporter，因此在生产和测试配置中显式设置 `management.prometheus.metrics.export.enabled=true`。另外，镜像默认 Entrypoint 已是 Prometheus/Alertmanager 主程序，校验命令必须使用 `--entrypoint promtool` / `--entrypoint amtool`，而不是把工具名作为普通参数。
- **影响文件**：Maven 依赖、生产/测试 Actuator 配置、Compose、Prometheus/Alertmanager/Grafana Provisioning、Dashboard JSON、`.env.example`、端点测试、README、CHANGELOG、VERIFY-007 报告。
- **验证方式**：执行 `docker compose config`、`promtool check config/rules`、`amtool check-config`、完整 `.\mvnw.cmd test` 和 `.\mvnw.cmd package -DskipTests`。保留用户 IntelliJ 的 8080 进程不动，在 18080 启动隔离应用，并用临时验证配置让 Prometheus 抓取该端口；将 Qwen Planner 指向未监听端口，失败阈值设为 2，发起两批各 12 个请求，再启动本地兼容响应模拟器验证恢复。
- **验证结果**：92 个测试通过，0 失败，0 错误；打包成功。Prometheus 配置和 4 条规则、Alertmanager 配置均通过官方工具检查。真实 Target 为 `UP`；Grafana 12.4.12 数据源健康并成功加载 `opsmind-agent` Dashboard；Alertmanager Ready 返回 200。首批 12 请求全部 200，记录 2 次远程 I/O 失败、1 次熔断打开、10 次熔断旁路和 12 次降级；第二批在抓取窗口内产生约 12 次请求/降级增量。`OpsMindPlannerCircuitOpen` 与 `OpsMindPlannerFallbackRateHigh` 均进入 firing 并送达 Alertmanager；恢复模拟器返回有效 Planner JSON 后，Provider 恢复为 `qwen`、熔断 Gauge 从 1 变为 0，熔断告警从活动列表消失。高降级率按 5 分钟滚动窗口保留，等待窗口衰减后自然恢复。
- **修改前指标**：0 个 Prometheus 抓取目标；0 个 Grafana Dashboard；0 条告警规则；91 个测试。
- **修改后指标**：1 个 Prometheus Target；1 个 7 面板 Dashboard；4 条告警规则；3 个本地监控入口；92 个测试。
- **修改效果**：Planner 的单次指标升级为可查询的时序数据；持续故障能自动触发规则、进入 Alertmanager 聚合/静默流程，并可在上游恢复后清除；本地演示环境通过一次 Compose 启动即可复现完整监控链路。
- **风险与后续**：当前 Alertmanager 只使用本地 UI 接收器，阈值来自本机故障注入，Prometheus 也是单实例绑定挂载。生产环境需要保护 Actuator 和监控端口、修改 Grafana 密码、接入真实通知渠道，并根据流量、错误预算和压测结果重新校准阈值。

## [FEAT-022] 增加 Planner 指数退避、熔断与可观测指标

- **日期**：2026-10-13
- **类型**：增强 / Agent / 韧性 / 可观测性
- **状态**：已完成；自动化测试与真实熔断故障注入验证通过
- **关联需求或问题**：继续完善项目第 23 步；FEAT-021 已提供超时、有限重试和本地降级，但重试立即执行，上游持续故障时每个请求仍会等待远程失败，且没有运行指标判断降级率和熔断状态。
- **问题**：连续故障会形成重复无效连接和延迟放大；没有 `CLOSED/OPEN/HALF_OPEN` 状态保护；运维侧只能逐条查看 Run，无法按时间窗口观察请求结果、耗时、重试、降级和熔断事件。
- **修改内容**：新增初始/最大退避、熔断失败阈值和开放时长配置；重试延迟按 2 的幂增长并封顶；新增同步保护的实例内三态熔断器，开放窗口结束只允许一个半开探针。接入 Micrometer，发布请求结果 Counter、端到端 Timer、尝试/重试/降级 Counter、熔断打开/旁路 Counter 与状态 Gauge；标签仅使用固定结果、原因和内部错误码。退避被中断时恢复线程中断标记并终止规划，不继续执行下游。新增退避序列、指标值、熔断旁路、半开恢复和中断语义测试。
- **实现中发现的问题**：首次新增的测试成功响应少了一层 JSON 字符串转义，被严格结构解析器正确判为非法响应并降级；修正 Fixture 后，第三次请求按预期由 Qwen 成功返回。这说明测试数据必须同时满足外层 Chat 响应 JSON 与内层 Planner JSON 两层编码。
- **影响文件**：Planner 配置、Qwen Planner、熔断器、`.env.example`、生产/测试配置、Planner/熔断测试、README、CHANGELOG、VERIFY-006 报告。
- **验证方式**：执行 Planner 定向测试和完整 `.\mvnw.cmd test`；以 PostgreSQL 17.11 启动 18080 临时实例，将 Qwen 地址指向本机未监听端口，设置最大尝试 1、失败阈值 2，连续调用三次 Agent API，并读取 Actuator metrics。
- **验证结果**：91 个测试通过，0 失败，0 错误。真实请求前两次分别耗时 323 ms、27 ms 并记录 `attempts=1`；第二次后熔断打开。第三次耗时 23 ms，记录 `AGENT_PLANNER_CIRCUIT_OPEN/attempts=0`。指标显示 attempts=2、requests=3、fallbacks=3、opened=1、rejected=1、state=1；三个 Run 均为 `COMPLETED`。
- **修改前指标**：固定立即重试；0 个熔断状态；0 个 Planner Micrometer 指标；87 个测试。
- **修改后指标**：2 个退避参数；3 态熔断器；8 类 Planner 指标；91 个测试；熔断旁路样本较首次请求端到端耗时从 323 ms 降至 23 ms。
- **修改效果**：短暂错误仍可通过重试恢复，持续错误在达到阈值后快速失败并转入本地规则，减少对故障上游的压力；运行人员可以通过 Actuator 区分 Qwen 成功、降级、重试与熔断旁路。
- **风险与后续**：熔断器当前为单 JVM 实例状态，退避未加随机抖动，也没有 Prometheus 告警和 Grafana 面板。样本耗时受本机与缓存影响，只证明旁路路径生效，不代表生产延迟。下一步增加指标告警与仪表盘，并评估 Resilience4j 或网关级跨实例治理。

## [FEAT-021] 增加 Qwen Planner 超时、重试与本地降级

- **日期**：2026-10-13
- **类型**：增强 / Agent / 韧性 / 降级 / 审计
- **状态**：已完成；自动化测试与真实故障注入验证通过
- **关联需求或问题**：继续完善项目第 22 步；FEAT-020 已优化本地路由质量，但 Qwen Planner 请求没有独立超时、重试和自动降级，上游短暂抖动会直接中断 Agent。
- **问题**：远程连接不可达、读取超时、HTTP 429 或 5xx 都会在第一次失败后终止；若盲目重试普通 4xx 会放大无效流量；若降级不写入审计，后续又无法区分 Qwen 决策和规则决策。
- **修改内容**：新增连接超时、读取超时、最大尝试次数、降级开关和降级模型配置；网络错误、429、5xx 才重试，普通 4xx 和非法结构化响应立即进入恢复路径；默认在两次远程尝试失败后调用 `local-intent-router-v2`，结果以 `local-fallback` Provider、降级模型、脱敏错误码和尝试次数持久化。部署配置校验位于恢复循环之前，缺少 Key/Base URL 时保留明确 503。新增 429 后成功、连续 5xx 后降级、401 不重试三个契约测试。
- **实现中发现的问题**：首次为配置 record 增加兼容构造器后，Spring Boot 配置绑定因出现多个构造器而报告 `No default constructor found`，导致应用上下文测试失败；移除重载构造器并统一更新测试构造参数后恢复类型安全绑定。
- **影响文件**：Planner 配置、Qwen Planner 适配器、`.env.example`、生产/测试配置、Planner 测试、README、CHANGELOG、VERIFY-005 报告。
- **验证方式**：执行完整 `.\mvnw.cmd test`；将 Qwen Planner 指向本机未监听端口，配置 200/300 ms 超时、最多 2 次尝试和本地降级，其余三个 AI Provider 使用 local；调用真实 Agent POST 后按 Run ID GET 回读持久化审计。
- **验证结果**：87 个测试通过，0 失败，0 错误。故障注入请求 438 ms 完成，状态 `COMPLETED`、任务 `KNOWLEDGE_QA`、Provider `local-fallback`、模型 `local-intent-router-v2`，理由记录 `AGENT_PLANNER_PROVIDER_UNAVAILABLE` 和 `attempts=2`；回读结果一致并保存 4 个步骤。
- **修改前指标**：0 个 Planner 专用超时参数；远程错误最多 1 次请求；0 条自动降级路径；84 个测试。
- **修改后指标**：2 个独立超时参数；1～3 次可配置尝试；3 类可重试瞬时故障；1 条带持久化审计的本地降级路径；87 个测试。
- **修改效果**：Qwen 短暂限流或服务异常可先重试，不可恢复的客户端错误不会产生重试风暴；远程持续不可达时 Agent 仍能以保守规则完成路由，并可从 Run 审计中准确识别降级原因。
- **风险与后续**：当前重试为固定次数立即执行，没有指数退避、抖动、熔断和 Provider 可用率指标；配置错误刻意不降级；本地 v2 的质量结论仍只来自开发回归集。下一步增加 Micrometer 指标和断路器，并冻结独立路由测试集分别评估 Qwen 与降级模型。

## [FEAT-020] 基于失败案例改进本地降级 Planner

- **日期**：2026-10-13
- **类型**：优化 / Agent / 规则路由 / 评测驱动
- **状态**：已完成；同版本开发回归集验证通过
- **关联需求或问题**：继续完成项目第 21 步；EVAL-007 量化发现 local Planner 的 Accuracy=0.8750、Macro-F1=0.8748，且危险写误路由率=0.1667。
- **问题**：v1 只判断“包含工单 + 创建动词”，导致“不要创建工单”与“创建工单前需要哪些审批”被错误送入写流程；动词表缺少“登记”，又漏掉真实写意图。简单关键词顺序无法区分否定、咨询和命令。
- **修改内容**：升级 `local-intent-router-v2`；对输入做 NFKC、大小写和空白归一化；按“否定写表达 → 工单流程咨询 → 写意图 → 默认知识问答”的优先级路由；新增否定窗口、如何创建/流程/审批问句模式，并扩展登记、建立、开/建一张工单等动作表达；不同分支返回具体审计理由。新增 2 个测试覆盖 5 种关键表达，并更新测试配置和离线配置说明。
- **影响文件**：`LocalAgentPlanner`、Planner 单元测试、测试配置、README、CHANGELOG、EVAL-008 报告。
- **验证方式**：执行完整 `\.\mvnw.cmd test`；使用真实评测 API 在同一 `opsmind-agent-routing-v1` 的 24 条开发回归样本上对比 v1/v2；确认无新增失败后关闭临时 18080 实例。
- **验证结果**：84 个测试通过，0 失败，0 错误。v2 的 24 条全部正确：Accuracy、Macro Precision/Recall/F1 均为 1.0000，危险写误路由 0/12；P50=0.0143 ms、P95=0.1512 ms。
- **修改前指标**：21/24 正确；Accuracy=0.8750；Macro-F1=0.8748；危险写误路由率=0.1667；82 个测试。
- **修改后指标**：24/24 正确；Accuracy=1.0000；Macro-F1=1.0000；危险写误路由率=0；84 个测试。
- **修改效果**：本地降级路径能够区分当前数据集中的否定、流程咨询和执行命令，已知危险误路由归零；审计理由也从统一文案细化为实际命中的语义分支。
- **风险与后续**：这是针对已观察失败集的开发回归结果，存在过拟合，不能写作生产准确率。规则仍不理解复杂否定范围、多轮上下文和隐含意图。下一步冻结独立测试集，先测后改；随后为 Qwen 与 local 使用同口径评测，并增加超时和降级策略。

## [FEAT-019] 建立 Agent Planner 路由评测闭环

- **日期**：2026-10-13
- **类型**：功能 / Agent / 评测 / 质量治理
- **状态**：已完成；本地 Planner 真实接口基线已运行
- **关联需求或问题**：继续完成项目第 20 步；FEAT-018 已接入 Planner，但只有协议和分支测试，没有固定数据集衡量任务选择质量，也无法发现危险的知识请求误路由到写任务。
- **问题**：单个成功示例不能代表意图路由质量；普通 Accuracy 也会掩盖类别不均衡和写误路由风险。若评测通过完整 Agent 接口执行，还会生成 Run、检索知识甚至产生待审批副作用。
- **修改内容**：新增白名单数据集 `opsmind-agent-routing-v1`，包含 24 条平衡样本，覆盖知识、已有工单咨询、否定句、流程问题、Prompt Injection、歧义输入、显式/间接写意图。新增 `POST /api/v1/evaluations/agent-routing/run`，直接调用 `AgentPlanner`，绝不执行工具。报告计算 Accuracy、Macro Precision/Recall/F1、每类 support/predicted/TP、危险写误路由数量和比率、P50/P95，并返回逐用例期望值、实际值、理由与类别。新增数据集安全加载、指标公式和 OpenAPI 测试。
- **实现中发现的问题**：真实 local 基线暴露 3 条失败：`qa-negation-01` 和 `qa-workflow-01` 因同时包含“创建+工单”被危险误路由；`ticket-indirect-01` 的“登记工单”未命中有限动词表。没有在同一数据集上立即改规则，以免把探索集结果伪装成泛化提升。
- **影响文件**：Agent 路由评测 domain/application/API、版本化 JSON 数据集、OpenAPI 与评测测试、README、CHANGELOG、EVAL-007 报告。
- **验证方式**：运行完整 `\.\mvnw.cmd test`；使用 PostgreSQL 17.11、`local-intent-router-v1` 在临时 18080 实例调用真实评测接口；确认接口没有 Agent Run、检索或写工具依赖，验证后关闭实例。
- **验证结果**：82 个测试通过，0 失败，0 错误。24 条真实路由中正确 21 条；Accuracy=0.8750、Macro Precision=0.8776、Macro Recall=0.8750、Macro-F1=0.8748；危险写误路由 2/12，Rate=0.1667；P50=0.0012 ms、P95=0.1301 ms。
- **修改前指标**：0 条固定路由样本；0 个 Agent 评测 API；无工具选择准确率和危险误路由指标；78 个测试。
- **修改后指标**：24 条平衡样本；1 个 Agent 评测 API；2 类分类指标 + 4 个整体质量指标 + 2 个延迟分位数；82 个测试。
- **修改效果**：项目首次以可重复数据衡量 Planner，而不是只展示成功案例；同时将“知识请求错误进入写流程”作为独立风险指标，并暴露可定位的失败类型。
- **风险与后续**：24 条样本规模小且当前只有 local 基线；尚未运行 Qwen，也没有开发/测试集隔离。下一步扩充并拆分数据集，在开发集改进否定句、咨询语义和间接动词，再只在独立测试集报告最终指标；随后校准 Qwen 并记录成本。

## [FEAT-018] 接入 Qwen 结构化 Planner 与受控任务路由

- **日期**：2026-10-13
- **类型**：功能 / Agent / 模型接入 / 审计
- **状态**：已完成；自动化测试与 PostgreSQL 真实接口验证通过
- **关联需求或问题**：继续完成项目第 19 步；FEAT-017 的 `taskType` 仍由调用方指定，省略后固定走知识问答。
- **问题**：调用方必须知道内部任务枚举才能创建工单；已有审计无法判断任务是模型选择还是调用方覆盖。直接让模型返回工具名并执行还可能绕过服务端风险策略。
- **修改内容**：新增 `AgentPlanner` 抽象以及 Qwen/local 实现；Qwen 使用温度 0、关闭 thinking、请求严格 JSON，只允许 `KNOWLEDGE_QA`、`CREATE_TICKET`。服务端拒绝未知字段、未知枚举和格式外文本。Planner 只做路由，状态机与 `AgentToolPolicy` 决定工具和权限。保留显式 `taskType` 调试覆盖。Flyway V8 持久化来源、Provider、模型和理由，并同步更新 API、Swagger、配置模板、README、VERIFY-004 与测试。
- **实现中发现的问题**：V8 最初用一条 `ALTER TABLE` 添加四列，PostgreSQL 可用但 H2 2.4 测试库报错；拆成四条语句后兼容两种数据库。真实 local 验证时模型名继承 Qwen 默认值，因此补充独立 `AGENT_PLANNER_MODEL`，避免离线审计含糊。
- **影响文件**：Agent Planner/config/domain/application/infrastructure/API、Flyway V8、配置模板、测试、文档与验证报告。
- **验证方式**：执行完整 `\.\mvnw.cmd test`；在 PostgreSQL 17.11 上迁移 V8，以四个 local Provider 启动 18080 临时实例，调用自动知识问答、自动创建工单、拒绝审批和显式覆盖接口。
- **验证结果**：78 个测试通过，0 失败，0 错误。真实接口健康状态 `UP`；知识请求为 `KNOWLEDGE_QA/PLANNER/COMPLETED`；创建请求为 `CREATE_TICKET/PLANNER/WAITING_APPROVAL`，拒绝后 `REJECTED` 且无 Ticket；显式覆盖为 `KNOWLEDGE_QA/EXPLICIT/COMPLETED`。
- **修改前指标**：0 个 Planner；任务省略时固定知识问答；0 个路由来源字段；71 个测试。
- **修改后指标**：2 个 Planner Provider；2 种受控任务类型；2 种任务来源；4 个持久化路由字段；78 个测试。
- **修改效果**：Agent 从“调用方替模型选任务”升级为“模型提出受限任务、服务端按确定性策略执行”，并保持写操作审批、幂等和审计边界。
- **风险与后续**：Qwen 契约已验证，但还没有独立标注集的模型路由准确率；Planner 调用失败发生在 Run 创建前。下一步建立路由评测集，统计 Macro-F1 与危险误路由率，并增加 Planner 超时、重试、降级和失败审计。

## [FEAT-017] 建立写工具与 Human-in-the-loop 审批闭环

- **日期**：2026-10-13
- **类型**：功能 / Agent / Human-in-the-loop / 幂等 / 审计
- **状态**：已完成；H2 自动化回归和 PostgreSQL 17.11 真实接口验证已通过
- **关联需求或问题**：继续完成项目第 18 步；FEAT-016 只有只读 `knowledge_search`，虽然定义了写风险级别，却没有真实写工具、待审批状态、批准/拒绝接口或幂等副作用，无法证明 Agent 能安全执行有业务影响的操作。
- **问题**：让 Agent 直接执行创建工单会把模型或路由错误转化为真实副作用；只在内存中等待确认又会在重启后丢失上下文。重复点击批准、网络重试和并发请求还可能创建多张相同工单。
- **修改内容**：
  - 将 `AgentTool` 重构为泛型输入/输出契约；保留只读 `knowledge_search`，新增风险为 `WRITE_REQUIRES_APPROVAL` 的 `create_ticket`。
  - 请求新增显式 `taskType`：`KNOWLEDGE_QA` 保持兼容并默认执行，`CREATE_TICKET` 先检索知识、生成 P2 草稿，然后进入 `WAITING_APPROVAL`，审批前写工具执行次数为 0。
  - 新增 `POST /api/v1/agent/runs/{runId}/approval`，支持 `APPROVE`/`REJECT` 和可选原因；批准生成 `OPEN` 工单，拒绝以 `REJECTED` 完成且不产生工单。
  - 新增 Flyway V7：`agent_run.task_type`、`agent_approval`、`support_ticket`；Run、审批、草稿、决定、工单和 1~7 状态步骤均可按 Run ID 回读。
  - 审批决定、工单写入、步骤追加和 Run 完成处于同一 Spring JDBC 事务；草稿审批记录、`WAITING_APPROVAL` 状态和 `APPROVAL_REQUESTED` 步骤也在一个仓储事务中提交。
  - `support_ticket.agent_run_id` 使用唯一约束提供最终幂等边界；顺序重复提交相同决定直接返回已有结果，相反决定返回 409。
  - Swagger 增加知识问答/创建工单双示例、审批接口和响应模型；新增应用层、JDBC、真实 HTTP 与 OpenAPI 测试；新增 VERIFY-003 报告。
- **实现中发现的问题**：初版先写审批步骤、再写审批记录和 Run 状态，数据库异常时存在半完成审计窗口；随后将三项合并到 `requestApproval` 事务。真实 PostgreSQL 验证还证明 `WAITING_APPROVAL` 时工单数保持 0，说明风险策略不是只显示一个提示，而是真正阻止了写入。
- **影响文件**：Agent domain/application/infrastructure/API、Flyway V7、Agent/JDBC/HTTP/OpenAPI 测试、`README.md`、`CHANGELOG.md`、VERIFY-003 报告。
- **验证方式**：执行定向测试、完整 `\.\mvnw.cmd test` 和打包；使用 PostgreSQL 17.11 + pgvector 0.8.7、本地三个 Provider，在临时端口依次执行创建草稿、批准、重复批准、创建第二个草稿和拒绝，并查询 `support_ticket`、`agent_step`。
- **验证结果**：71 个自动化测试通过，0 失败，0 错误。真实批准 Run 在草稿阶段为 `WAITING_APPROVAL/PENDING`、4 个步骤、0 张工单；批准后为 `COMPLETED/APPROVED`、7 个步骤、1 张工单；重复批准返回同一 Ticket ID，数据库仍为 1 张。拒绝 Run 为 `REJECTED`、5 个步骤、0 张工单。
- **修改前指标**：1 个只读工具；2 个 Agent API；3 种 Run 状态；没有审批记录、写工具或工单表；65 个测试。
- **修改后指标**：1 个只读工具 + 1 个写工具；3 个 Agent API；5 种 Run 状态；2 张新增业务/审批表；批准链路 7 步、拒绝链路 5 步；71 个测试。
- **修改效果**：Agent 从“只能回答问题”升级为“先生成可审查计划，再由人确认并执行有副作用工具”，同时具备持久化等待、拒绝分支、事务一致性、幂等边界和完整审计。
- **风险与后续**：当前 `taskType` 由调用方显式指定，审批接口还没有登录身份、RBAC、过期租约与多实例并发友好重试；工单系统也是本地模拟表。下一步增加用户/租户身份与审批权限，记录审批人，加入审批过期和 Outbox，再用 Qwen 结构化输出选择受白名单约束的任务类型。

## [FEAT-016] 建立可审计的只读 Agent 运行闭环

- **日期**：2026-10-13
- **类型**：功能 / Agent / 工具治理 / 审计
- **状态**：已完成；本地 PostgreSQL 真实接口验证已通过
- **关联需求或问题**：继续完成项目第 17 步；FEAT-015 只有 RAG 问答接口，没有 Agent Run、工具风险边界、状态轨迹和持久化审计，简历中的 Agent 工程能力仍停留在设计文档。
- **问题**：直接由 Controller 调用 RAG 服务无法表达“接收任务、制定计划、调用工具、返回结果”的执行过程；后续加入工单写操作时也缺少统一的风险策略和审批边界。请求结束后没有 Run ID，故障与拒答过程无法按步骤回放。
- **修改内容**：
  - 新增确定性 Agent 状态机，按 `RECEIVED → PLANNED → TOOL_CALL → RESPONDED` 执行首个只读知识问答任务。
  - 定义统一 `AgentTool` 契约和 `knowledge_search` 实现，复用 `RagAnswerService` 的混合召回、精排、证据门禁、生成与引用链路；正常请求省略 `documentId`，自动从全部可检索文档中选择证据。
  - 新增 `READ_ONLY`、`WRITE_REQUIRES_APPROVAL`、`PROHIBITED` 风险级别和集中策略；只有只读工具可自动执行，写风险工具在调用前被拦截。
  - 新增 Flyway V6 的 `agent_run`、`agent_step` 表，保存运行状态、回答、拒答、引用、错误码和有序步骤；步骤只记录字符数、Token 数、引用数、分数等紧凑元数据，不保存完整 Prompt 或证据正文。
  - 新增 `POST /api/v1/agent/runs` 与 `GET /api/v1/agent/runs/{runId}`；Swagger 示例省略 `documentId`，同时修正原 RAG 示例，避免示例 UUID 被误当成真实文档范围。
  - 新增状态机、风险拦截、查询校验、JDBC 完成/失败回读和 OpenAPI 路径测试；新增 VERIFY-002 工程验证报告。
- **实现中发现的问题**：真实验证前错误使用了 `document` 表名，而项目实际表名为 `knowledge_document`；更正查询后确认数据库有 5 条文档记录、3 个 Child Chunk 和两套向量。Agent 请求不传 `documentId` 成功自动命中正确文档，说明先前 Swagger 中占位 UUID 导致 `NO_EVIDENCE`，不是 RAG 必须人工选文档。
- **影响文件**：Agent domain/application/infrastructure/API、Flyway V6、全局异常处理、RAG/Agent Swagger 示例、OpenAPI 与 Agent 测试、`README.md`、`CHANGELOG.md`、VERIFY-002 报告。
- **验证方式**：执行 Agent 定向测试、完整 `\.\mvnw.cmd test` 和打包；使用 PostgreSQL 17.11 + pgvector 0.8.7、本地三个 Provider，在临时端口调用不带 `documentId` 的 Agent POST，再按 `runId` GET 回读；追加域外问题验证拒答审计。
- **验证结果**：65 个自动化测试通过，0 失败，0 错误。真实可回答请求状态为 `COMPLETED`，自动命中 `feat007.md > OpsMind 运维手册 > 告警处置`，Top-1=0.289683，产生 4 个有序步骤；域外问题以 `LOW_RELEVANCE` 拒答，Top-1=0.039513，同样产生 4 个步骤。数据库共保存 2 个完成 Run 和 8 个 Step。
- **修改前指标**：0 个 Agent API；0 个工具契约；0 种风险策略；0 张 Agent 审计表；RAG 请求易误填 Swagger 占位文档 UUID。
- **修改后指标**：2 个 Agent API；1 个可执行只读工具；3 个风险级别；2 张审计表；4 阶段可回放状态轨迹；5 个新增自动化测试。
- **修改效果**：项目从“直接调用 RAG 服务”升级为“有运行身份、有工具边界、有状态轨迹、有持久化审计”的 Agent 最小闭环，并保持实际问答自动选文档。
- **风险与后续**：当前 Planner 是确定性单工具路由，`COMPLETED` 表示工具链正常结束，回答仍可能是有依据的拒答；还没有多工具循环、并发状态控制、写工具审批、恢复与预算。下一步新增工单草稿工具和 `WAITING_APPROVAL` 状态，并用幂等键保护审批后的写操作。

## [FEAT-015] 增加证据充足性门禁与低相关度拒答

- **日期**：2026-10-13
- **类型**：功能 / RAG / 幻觉治理 / 评测
- **状态**：已完成；本地 20 条真实数据库评测已通过
- **关联需求或问题**：继续完成项目第 16 步；EVAL-005 中两个域外问题都被强行回答，Refusal F1=0，证明“有 Top-K 候选”不等于“有足够证据”。
- **问题**：向量/混合检索即使面对知识库外问题也通常会返回最相似结果；旧逻辑只在候选为空时拒答，因此会把弱相关片段发送给模型并生成带有伪引用的回答。
- **修改内容**：
  - 在 `RagAnswerService` 调用生成 Provider 之前增加证据充足性门禁；无候选返回 `NO_EVIDENCE`，Top-1 精排分低于配置阈值返回 `LOW_RELEVANCE`，两种情况都不调用 Chat 模型。
  - 新增 `MINIMUM_EVIDENCE_SCORE`，默认 0.15，并校验为有限的 0~1 数值；该配置随 `.env.example` 暴露。
  - RAG 回答和逐用例评测响应新增 `refusalReason` 与 `topEvidenceScore`，使拒答原因、实际分数和阈值可观测。
  - 保持 `opsmind-answer-v1` 的 8 条历史数据不变；新增默认 `opsmind-answer-v2`，扩展到 6 个正样本与 14 个人事、账号、基础设施和流程类负样本。
  - 新增低相关度不调用生成器的单元测试，更新配置绑定、Provider 契约、评测映射和数据集加载测试；新增 EVAL-006 报告。
- **实现中发现的问题**：首次扩展时直接修改了 v1 文件，会破坏 EVAL-005 的可复现性；在提交前改为保留 v1 并新建 v2。当前本地正样本最低 Top-1=0.179、负样本最高=0.125，0.15 能分开现有样本，但 v2 同时参与了探索和验证，结果存在过拟合。
- **影响文件**：生成配置、RAG 回答结果/响应/服务、拒答原因枚举、回答评测逐用例模型、v2 数据集、配置模板、相关测试、`README.md`、`CHANGELOG.md`、EVAL-006 报告。
- **验证方式**：执行定向测试、完整 `\.\mvnw.cmd test` 和打包；使用 PostgreSQL 17.11 + pgvector 0.8.7、本地三个 Provider、限定文档调用真实 `opsmind-answer-v2` 评测接口。
- **验证结果**：60 个自动化测试通过，0 失败，0 错误。20 条真实顺序请求中，6 个正样本全部回答，14 个负样本全部以 `LOW_RELEVANCE` 拒答；Answer Correctness、Citation Precision/Recall、Refusal Precision/Recall/F1 均为 1.000，P50=2.57 ms、P95=4.75 ms。
- **修改前指标**：8 条回答样本；2 个负样本均错误回答；Citation Precision=0.750；Refusal F1=0；拒答响应不含原因和分数。
- **修改后指标**：20 条回答样本；14 个负样本均拒答；Citation Precision=1.000；Refusal F1=1.000；拒答原因与 Top-1 分数可观测；低相关问题不产生生成 Token。
- **修改效果**：系统从“检索有结果就生成”升级为“证据通过门禁才生成”，减少域外幻觉、伪引用和无效模型费用，同时保留可配置和可回归的工程边界。
- **风险与后续**：1.000 仅是当前单文档、本地模型、小样本的探索结果，不能写成生产准确率；不同 Reranker 的分数不可直接共用阈值。下一步增加多文档并划分独立校准/测试集，获得授权后单独校准 Qwen；随后实现 Agent 工具调用、风险分级和审计。

## [FEAT-014] 建立带引用回答质量与拒答评测基线

- **日期**：2026-10-13
- **类型**：功能 / 评测 / RAG / 质量治理
- **状态**：已完成；本地真实数据库基线已运行
- **关联需求或问题**：继续完成项目第 15 步；FEAT-013 只有一条成功问答和引用格式验证，无法量化 Prompt、模型或拒答策略修改对事实覆盖、引用质量和域外幻觉的影响。
- **问题**：检索指标不能替代最终回答指标；“回答带引用”也不能证明引用正确、事实完整或域外问题会被拒答。若没有固定的正负样本、统一指标和逐用例结果，后续容易只展示成功案例并掩盖失败。
- **修改内容**：
  - 新增白名单数据集 `opsmind-answer-v1`，包含 6 个可回答运维问题和 2 个域外不可回答问题；可回答样本标注事实组与标题路径，事实组支持同义短语。
  - 新增 `POST /api/v1/evaluations/answers/run`，支持固定 `documentId` 和可选 `topK`，复用生产 `RagAnswerService` 跑完整的召回、精排、上下文与生成链路。
  - 计算 Answer Correctness、Citation Precision/Recall、Refusal Precision/Recall/F1、P50/P95、Prompt/Completion/Total Token，并返回逐用例事实命中、引用标题和答案。
  - 新增 `faithfulnessProxy`：用回答字符 bigram 在实际引用摘录中的覆盖率提供完全确定性的词面回归信号；API Schema、README 和报告均明确它不是语义 Faithfulness Judge。
  - 数据集名称使用静态白名单映射，不允许根据请求值读取任意 classpath 路径；严格校验 ID 唯一性、Top-K、可回答/不可回答标注一致性和空事实组。
  - 新增数据集加载、安全边界、指标公式、错误拒答暴露、Top-K 校验与 OpenAPI 测试；新增 EVAL-005 基线报告。
- **实现中发现的问题**：真实本地基线中，6 个可回答用例的事实覆盖和引用召回均为 1.000，但两个域外问题都被强行回答并错误引用“恢复验证”，导致 Citation Precision=0.750、Refusal F1=0.000。根因是向量/混合检索总会返回 Top-K，而当前回答服务只在候选为空时拒答。
- **影响文件**：回答评测领域模型、数据集加载器、评测服务与 API、版本化 JSON 数据集、评测与 OpenAPI 测试、`README.md`、`CHANGELOG.md`、EVAL-005 报告。
- **验证方式**：运行定向测试和完整 `\.\mvnw.cmd test`；使用 PostgreSQL 17.11 + pgvector 0.8.7、限定文档、本地 Embedding/Reranker/Generation Provider 调用真实回答评测 API。
- **验证结果**：59 个自动化测试通过，0 失败，0 错误。8 条真实顺序请求的 Answer Correctness=1.000、Faithfulness Proxy=1.000、Citation Precision=0.750、Citation Recall=1.000、Refusal F1=0.000、P50=2.53 ms、P95=40.29 ms。
- **修改前指标**：1 条回答工程验证；无版本化回答数据集；无事实、引用、拒答、Token 或逐用例评测。
- **修改后指标**：8 条版本化回答用例；7 类聚合质量/拒答指标；2 类延迟分位数；3 类 Token 统计；逐用例可诊断结果；5 个新增自动化测试。
- **修改效果**：项目具备可重复的回答层回归门槛，并第一次用数据证明当前拒答策略存在缺陷；没有通过拍脑袋阈值或删除失败样本美化结果。
- **风险与后续**：数据集仍很小，本地抽取生成器天然会获得高词面支撑分，不能代表 Qwen 的语义质量。下一步先扩充困难负样本，再用独立验证集校准证据充足性阈值；随后增加固定版本的 Qwen Judge/人工抽检，并开始实现 Agent 工具调用与审计。

## [FEAT-013] 生成带引用的 Qwen RAG 回答

- **日期**：2026-10-11
- **类型**：功能 / RAG / 模型接入 / 安全
- **状态**：已完成；Qwen HTTP 契约与本地真实端到端链路已验证
- **关联需求或问题**：继续完成项目第 14 步；FEAT-012 只能返回检索结果，调用方仍需自行拼接上下文、调用 Chat 模型并解释来源，尚未形成用户可消费的问答接口。
- **问题**：直接把 Top-K Child 文本拼给模型会丢失上下文并重复发送同一 Parent；自由文本回答可能没有来源或引用不存在的证据；文档正文还可能包含 Prompt Injection。没有证据时继续调用模型也会产生幻觉和无效费用。
- **官方依据**：[阿里云百炼 Qwen 文本生成 API](https://help.aliyun.com/zh/model-studio/qwen-api-reference/)提供 OpenAI 兼容 Chat Completions 接口，使用 `model/messages` 生成回答并在 `usage` 返回 Token 用量。本项目默认 `qwen3.7-plus`，显式关闭 thinking 以控制首版延迟和成本边界。
- **修改内容**：
  - 新增 `AnswerGenerationProvider` 抽象、生产 `QwenChatAnswerGenerationProvider` 与仅供测试/演示的本地抽取 Provider；配置支持生成模型、独立 Base URL、证据数、上下文字符预算、输出 Token 上限和温度。
  - 新增 `POST /api/v1/rag/answer`：复用精排检索，按 Parent ID 去重，优先把完整 Parent 正文交给生成模型，并在 12000 字符预算内截断。
  - 证据使用 `S1/S2/...` 稳定编号；回答至少包含一个合法 `[S数字]`，未知编号、空回答或无引用回答均返回 `502 ANSWER_PROVIDER_INVALID_RESPONSE`；响应只携带实际使用的引用。
  - 引用对象返回文档名、文档 ID、Child/Parent Chunk ID、标题路径、原文摘录和精排分数，便于前端展示与审计。
  - 零证据时不调用生成模型，直接返回固定中文拒答；System Prompt 将 evidence JSON 视为不可信数据，禁止执行文档指令、改变角色或使用模型记忆补造内部事实。
  - Qwen 使用 `{QWEN_CHAT_BASE_URL}/chat/completions`、Bearer 鉴权和 `enable_thinking=false`；缺少配置返回 503，上游异常映射为不含响应正文的 502，Token 用量随 API 响应返回。
  - 新增应用服务测试、Qwen Mock HTTP 契约测试、OpenAPI 路径测试和文档上传至回答的 HTTP 端到端测试；新增 EVAL-004 工程验证报告。
- **实现中发现的问题**：Reranker relevance score 只表示当前 Query-Passage 请求内的相对相关性，不适合未经校准就作为所有问题共用的拒答阈值，因此首版只在零证据时拒答。引用编号的语法校验也不能证明结论被证据蕴含，必须用独立 Faithfulness/Citation 指标评测，不能把“有引用”宣传成“无幻觉”。
- **影响文件**：生成配置、RAG 应用模型与服务、Qwen/本地生成 Provider、RAG API、`.env.example`、测试配置、应用/HTTP/OpenAPI/Qwen 契约测试、`README.md`、`CHANGELOG.md`、EVAL-004 报告。
- **验证方式**：执行 `\.\mvnw.cmd test`；使用 PostgreSQL 17.11 + pgvector 0.8.7 与三个本地确定性 Provider 启动真实应用，调用指定文档的 `/api/v1/rag/answer`；使用 Mock HTTP 服务验证 Qwen 请求与安全错误边界。
- **验证结果**：54 个自动化测试通过，0 失败，0 错误。真实本地请求“回滚完成后如何确认核心功能正常？”返回 `[S1]`，命中 `feat007.md > OpsMind 运维手册 > 发布回滚`，引用成功回映到对应 Child、Parent 和文档。
- **修改前指标**：3 个检索接口；48 个自动化测试；没有最终回答、引用校验、上下文预算、确定性拒答或 Chat 模型协议边界。
- **修改后指标**：1 个带引用回答接口；2 个可切换生成 Provider；6 个新增测试；54 个自动化测试；1 份回答工程验证报告。
- **修改效果**：项目从“可检索证据”升级为“可生成、可引用、可拒答、可审计来源”的 RAG 闭环，同时保持模型供应商适配与业务编排解耦。
- **风险与后续**：真实 Qwen Chat 会产生费用并把所选证据发送到外部服务，本次未在没有数据外发授权的情况下执行。下一步扩充多文档问答/不可回答数据集，评测正确性、忠实度、引用精确率/召回率和拒答准确率，再校准低相关度拒答阈值并接入可观测指标。

## [FEAT-012] 接入 Qwen Cross-Encoder Reranker 精排

- **日期**：2026-10-11
- **类型**：功能 / RAG / 模型接入 / 评测
- **状态**：已完成；Qwen HTTP 契约已验证，本地端到端评测已完成
- **关联需求或问题**：继续完成项目第 13 步；FEAT-011 的 RRF 只能融合候选名次，不会联合理解用户问题和候选段落，难以区分“主题相关”与“真正回答问题”。
- **问题**：Dense/BM25 属于高召回阶段，RRF 分数只由名次产生；若直接把少量 RRF Top-K 交给生成模型，困难负样本可能占用上下文预算。Embedding 与 Rerank 又使用不同协议和 URL，不能复用同一个调用实现。
- **官方依据**：[阿里云百炼文本排序文档](https://help.aliyun.com/zh/model-studio/text-rerank-api)说明 `qwen3.7-text-rerank` 面向语义检索和 RAG，最多处理 500 个文档；请求使用 `input.query/documents` 和 `parameters.top_n/instruct`，响应通过原始 `index` 与 `relevance_score` 关联候选。
- **修改内容**：
  - 新增 `RerankingProvider` 抽象以及生产 `QwenRerankingProvider`，默认模型为 `qwen3.7-text-rerank`；新增本地 Token Overlap Provider 供离线测试，不冒充生产 Cross-Encoder。
  - 新增独立 `QWEN_RERANK_URL`；不从 Embedding URL 猜测路径。缺少 URL/Key 返回 503，上游 HTTP/网络错误映射为脱敏 502。
  - 严格校验返回结果数量、候选索引范围、重复索引、有限分数及 0~1 范围，再按原始索引映射回 Chunk，避免模型结果与候选错位。
  - 新增 `RerankedSearchService`：先按 `max(20, topK×4)` 获取混合候选，再精排并截取用户请求的 Top-K；最终保留 Child 命中、Parent 上下文和来源字段。
  - 新增 `POST /api/v1/search/reranked`；score 表示当前请求内的 Rerank 相关性，不允许与余弦或 RRF 分数直接比较。
  - 评测策略扩展为 `VECTOR/HYBRID/RERANKED`，新增 EVAL-003 报告和 OpenAPI、服务编排、Qwen HTTP 契约、错误脱敏及端到端接口测试。
- **实现中发现的问题**：官方 `qwen3-rerank` 与 `qwen3.7-text-rerank` 的 URL、请求体和响应结构不同；本项目选用后者并按其 DashScope 嵌套协议实现。现有 3 候选/6 查询数据集在 Hybrid 阶段已达到 Top-1 100%，无法展示精排质量增益，因此没有虚构提升数字。
- **影响文件**：Reranker 配置与 Provider、重排应用服务、搜索 API、评测策略、Qwen 契约测试、服务与 HTTP 集成测试、`.env.example`、`README.md`、`CHANGELOG.md`、EVAL-003 报告。
- **验证方式**：执行 `\.\mvnw.cmd test`；使用 PostgreSQL 17.11 + pgvector 0.8.7、本地 Embedding 和本地确定性 Reranker，调用真实 `RERANKED` 评测接口；Mock HTTP 服务验证 Qwen 请求 JSON、Bearer 鉴权、乱序候选索引恢复和上游错误脱敏。
- **验证结果**：48 个自动化测试通过，0 失败，0 错误。RERANKED 的 Recall@1/MRR@1/HitRate@1 与 @3 三项均为 1.000，P50/P95 为 2.57/42.06 ms；与 Hybrid 相比质量持平，符合当前数据集已经饱和的预期。
- **修改前指标**：Dense + BM25 + RRF 后直接返回；43 个测试；评测仅支持 VECTOR/HYBRID。
- **修改后指标**：增加独立精排阶段、3 类评测策略、1 份精排报告、48 个测试；供应商错误和候选索引均有防御性校验。
- **修改效果**：项目形成“宽召回 → RRF 融合 → 精排 → Parent 上下文”的完整多阶段检索结构，为下一步带引用的 RAG 生成提供更干净的证据集合。
- **风险与后续**：真实 Qwen 调用会发送 Query 和候选 Child 文本并产生费用，需要明确的数据外发授权；当前未执行真实 Qwen 质量评测。下一步扩展多文档困难负样本，并实现带来源引用、无证据拒答的 Qwen Chat 回答接口。

## [FEAT-011] 增加 BM25 + pgvector + RRF 混合检索

- **日期**：2026-10-10
- **类型**：功能 / RAG / 检索优化 / 评测
- **状态**：已完成
- **关联需求或问题**：继续完成项目第 12 步；EVAL-001 发现本地向量模型对“回滚完成后的功能验证”语义改写召回不足，正确章节只排第 3，单路 Dense 检索也容易漏掉专有名词、错误码和精确短语。
- **问题**：只依赖向量相似度无法稳定利用标题和正文中的明确关键词；直接相加余弦分数与 BM25 分数又会混合两个不可比分数空间。
- **修改内容**：
  - 新增中文字符 unigram/bigram 分词器，对拉丁字母与数字保留归一化完整词；基于标题路径与 Child 正文实现标准 BM25（k1=1.2、b=0.75）。
  - 保留 pgvector Dense Top-K，双路各自召回候选后使用 Reciprocal Rank Fusion（RRF，默认 k=60）按排名融合，并按 Chunk ID 去重。
  - 候选池默认取 `max(20, topK × 4)`，同时受全局 `maxTopK=50` 限制；参数通过 `opsmind.search.hybrid` 集中配置。
  - 新增 `POST /api/v1/search/hybrid`，返回 Child 命中与 Parent 上下文；响应模型名称包含双路模型，score 明确定义为 RRF 分数而非余弦相似度。
  - 评测请求增加 `VECTOR/HYBRID` 策略，缺省仍为 `VECTOR` 保持兼容；评测响应记录实际策略和融合模型。
  - 新增分词、BM25 排序、RRF 融合去重测试，并把混合接口纳入 OpenAPI 契约测试。
  - 新增 `EVAL-002-hybrid-search.md`，记录同一数据集下修改前后指标、失败修复、性能解释和工程边界。
- **实现中发现的问题**：最初 BM25 会把零分候选也按 UUID 排序返回，RRF 因而可能给完全无词法证据的文档增加随机排名权重；改为只返回 BM25 分数大于 0 的候选。Qwen 评测会把本地语料查询发送到外部供应商，本轮未获该数据外发的单独授权，因此没有绕过隐私保护，只记录本地可复现对照。
- **影响文件**：检索应用服务与配置、词法仓储及 JDBC 实现、混合检索 API、评测策略与响应、测试配置与 3 组测试、OpenAPI 测试、`README.md`、`CHANGELOG.md`、EVAL-002 报告。
- **验证方式**：执行 `\.\mvnw.cmd test`；连接 PostgreSQL 17.11 + pgvector 0.8.7，使用同一文档、同一 6 条固定查询，分别调用 VECTOR 与 HYBRID 评测。
- **验证结果**：43 个自动化测试通过，0 失败，0 错误。Local Vector 的 Recall@1/MRR@1/HitRate@1 均为 0.833，Hybrid 三项均为 1.000；`rollback-verification` 的正确章节从第 3 提升至第 1。Hybrid P50/P95 为 2.81/13.92 ms。
- **修改前指标**：单路 pgvector Dense Top-K；本地模型 Top-1 三项指标 0.833；1 个固定失败用例；40 个自动化测试。
- **修改后指标**：Dense + BM25 双路召回与 RRF；本地模型 Top-1 三项指标 1.000，提升 16.7 个百分点；固定失败用例修复；43 个自动化测试。
- **修改效果**：项目从单路向量搜索升级为可配置、可评测的混合检索链路，能够解释“词法证据如何补偿语义模型”，并保留 Parent-Child 上下文展开和数据库侧向量 Top-K。
- **风险与后续**：当前 BM25 会从 PostgreSQL 读取限定范围的全部 Child Chunk 并在 JVM 计算，只是融合策略基线，不适合大规模生产；后续迁移到 Elasticsearch/OpenSearch 倒排索引、并行双路召回，并扩展至少 100 条多文档困难样本后再调参。P95 仅 6 次顺序请求，不能视为压测结论。

## [EVAL-001] 建立固定检索评测集与 Qwen/本地模型对照基线

- **日期**：2026-10-09
- **类型**：评测 / RAG / API / 回归测试
- **状态**：已完成
- **关联需求或问题**：继续完成项目第 11 步；此前只有单查询 Top-1 验证，无法量化模型或检索策略修改后的质量退化，也无法形成简历中可追溯的真实指标。
- **问题**：一次人工查询容易产生选择偏差；没有版本化语料、标注集、统一 K 值、指标公式和逐用例结果，后续更换模型、分块或引入混合检索时无法同口径比较。
- **修改内容**：
  - 新增可提交的演示语料 `evaluation/corpus/opsmind-operations.md`，并新增 `opsmind-retrieval-v1` JSON 数据集，包含 6 条中文语义改写查询、稳定用例 ID、相关标题路径以及固定 K=1/3。
  - 以标题路径而不是数据库 Chunk UUID 作为相关性标注，使重新上传、重新分块后仍可复现评测。
  - 新增数据集白名单加载器，未知名称直接返回 `404 EVALUATION_DATASET_NOT_FOUND`，不根据用户输入拼接任意 classpath 路径。
  - 新增评测服务，一次检索最大 K 后同时计算宏平均 Recall@K、MRR@K、HitRate@K、平均候选数、P50/P95 端到端检索延迟，并保留每个用例的召回标题和首个相关结果排名。
  - 新增 `POST /api/v1/evaluations/retrieval/run` 与 Swagger 文档；请求可限定文档 UUID，生产 Qwen 运行会明确产生外部调用费用。
  - 新增数据集解析、安全边界和确定性指标单元测试；纳秒时间源可注入，测试不依赖真实机器耗时。
  - 新增版本化对照报告 `evaluation/reports/EVAL-001-retrieval-baseline.md`，记录环境、口径、结果、失败案例、限制和复现步骤。
- **实现中发现的问题**：Spring Boot 4 当前依赖组合没有注册 `com.fasterxml.jackson.databind.ObjectMapper` Bean；首版通过构造器注入导致所有 Spring 上下文测试失败。加载器改为持有模块专用 ObjectMapper，不依赖 Web JSON 自动配置后恢复。
- **影响文件**：`evaluation` API/应用/领域模型、全局异常处理、版本化 JSON 数据集、演示语料、评测报告、OpenAPI 测试、评测单元测试、`README.md`、`CHANGELOG.md`
- **验证方式**：运行评测模块定向测试与 `.\mvnw.cmd test -q`；在 PostgreSQL 17.11 + pgvector 0.8.7 上对同一文档分别启动 Qwen 1024 维和本地 256 维 Provider，调用真实评测 API；额外验证路径型未知数据集名称返回安全 404。
- **验证结果**：40 个自动化测试通过，0 失败，0 错误。6 条真实查询中，Qwen 的 Recall@1/MRR@1/HitRate@1 均为 1.000，P50/P95 为 170.77/420.14 ms；本地模型的 @1 三项指标均为 0.833，Recall@3/HitRate@3 为 1.000、MRR@3 为 0.889，P50/P95 为 1.64/24.02 ms。
- **修改前指标**：1 条手工查询；只有 Top-1 命中和单次延迟；无固定数据集、无 MRR/Recall、无失败案例清单、37 个自动化测试。
- **修改后指标**：1 份版本化语料、1 份 6 用例数据集、2 个固定 K、3 类质量指标、2 类延迟分位数、1 份 Qwen/本地对照报告、40 个自动化测试。
- **修改效果**：模型质量与延迟取舍开始可量化回归；确认 Qwen 在当前语义改写小样本上比本地词面基线高 16.7 个百分点的 Top-1 命中率，并定位到“回滚完成后的验证”这一具体失败用例。
- **风险与后续**：当前只有 1 篇文档、3 个 Chunk、6 个查询，指标不能写成生产准确率；P95 是 6 次请求的 nearest-rank 观测，不是压测结果。下一步扩展多文档困难样本，并实现 BM25 + Vector + RRF，对失败用例做同口径对照。

## [FEAT-010] 使用 pgvector 执行数据库侧 Top-K 向量检索

- **日期**：2026-10-09
- **类型**：功能 / RAG / PostgreSQL / 性能架构
- **状态**：已完成
- **关联需求或问题**：继续完成项目；解决 FEAT-008 中“把当前模型的全部向量加载到 JVM，再逐条计算余弦相似度”的扩展性问题。
- **问题**：旧检索路径的网络传输量、JVM 内存和计算时间都会随候选 Chunk 数线性增长，数据库只负责过滤，不负责 Top-K；文本序列化向量也无法使用专用向量索引。
- **根因**：FEAT-008 为了先建立无外部依赖的可复现基线，使用 `TEXT` 保存向量，并把余弦计算留在应用层；原 `postgres:17-alpine` 镜像不包含 pgvector 扩展。
- **官方依据**：[pgvector 官方项目](https://github.com/pgvector/pgvector)支持 `vector` 类型、`<=>` 余弦距离、精确/近似最近邻检索和 HNSW 索引；官方 Docker 镜像提供 PostgreSQL 17 标签。`vector` 的 HNSW 索引最多支持 2000 维，因此当前 Qwen 1024 维可直接索引。
- **修改内容**：
  - Compose 数据库镜像固定为 `pgvector/pgvector:0.8.7-pg17-bookworm`，继续复用项目内 `data/postgres` 绑定目录，不重建数据库数据。
  - 新增 PostgreSQL 专属 Flyway V5：启用 `vector` 扩展，为 `chunk_embedding` 增加原生 `vector_value`，从 `vector_data` 无损回填既有向量并设为非空。
  - 为 `qwen3.7-text-embedding / 1024维` 和 `local-feature-hashing-v1 / 256维` 分别建立部分 HNSW 余弦索引；同一表继续支持不同模型、不同维度共存。
  - `ChunkEmbeddingRepository` 新增数据库侧 `findNearest` 契约；PostgreSQL 使用 `<=>` 排序和 `LIMIT`，只向 JVM 返回 Top-K 结果与 Parent 上下文，不再返回全部向量。
  - 候选总数通过独立轻量 `COUNT` 查询获得；Top-K SQL 不包含窗口聚合，避免为了 `candidateCount` 强制扫描并排序全部候选。
  - Top-K 子查询只绑定一份查询向量并计算一次距离，外层将 `distance` 转为相似度 `score`，避免重复传输和重复表达式计算。
  - 写入时同时维护原生向量列与文本兼容列；H2 测试环境保留仓储内确定性余弦兼容路径，并通过独立 Flyway location 隔离 PostgreSQL 方言迁移。
  - 相似度计算从应用服务下沉到仓储/数据库，`SemanticSearchService` 只负责编排查询向量、参数校验与响应映射。
- **实现中发现的问题**：
  - Flyway 会递归扫描 `db/migration`，最初把 PostgreSQL 专属 V5 放在其子目录仍被 H2 发现，导致 `CREATE EXTENSION` 语法失败；改为独立的 `db/postgresql-migration`，测试配置只扫描通用目录。
  - 首版 SQL 使用 `COUNT(*) OVER()` 同时返回候选数，虽然功能正确，但会削弱 HNSW 的提前截断价值；改为 `COUNT + Top-K` 两条查询，使距离查询保持纯 `ORDER BY distance LIMIT K` 结构。
  - 使用子查询向量进行 `EXPLAIN` 时规划器无法匹配 HNSW；改用与 JDBC 参数等价的固定向量验证后，执行计划命中 `idx_chunk_embedding_qwen_hnsw`。
- **影响文件**：Compose 镜像、Flyway 配置与 V5、向量编码器、Embedding 仓储契约与 JDBC 实现、检索服务、领域结果模型、H2 测试配置、仓储测试、`README.md`、`CHANGELOG.md`
- **验证方式**：执行 `.\mvnw.cmd clean test -q` 和最终 `.\mvnw.cmd test -q`；重建真实 PostgreSQL 容器并保留原绑定数据；启动生产 Qwen 配置执行 V5、真实向量重建和搜索；使用 SQL 核对扩展、迁移、回填、维度、索引，并用 `EXPLAIN` 验证 HNSW 路径。
- **验证结果**：37 个自动化测试通过，0 失败，0 错误；PostgreSQL 17.11 成功安装 pgvector 0.8.7 并迁移到 V5；6 条历史向量全部回填，`vector_value` 空值为 0，维度覆盖 256 和 1024；两个 HNSW 索引均存在。真实 Qwen 重建写入 3 条 1024 维向量；查询“数据库连接池告警如何处置”返回 3 个候选和 3 个结果，Top-1 仍为“OpsMind 运维手册 > 告警处置”，得分约 0.867，并返回 Parent 上下文。
- **修改前指标**：每次搜索加载当前模型的全部候选向量到 JVM，JVM 执行全部余弦计算；向量仅以 `TEXT` 保存；无专用向量索引。
- **修改后指标**：JVM 每次只接收最多 `topK` 条命中（默认 5、最大 50）；PostgreSQL 原生保存向量并执行余弦 Top-K；2 个按模型与维度隔离的 HNSW 索引；历史向量 100% 回填成功。
- **修改效果**：检索边界从“应用层全量扫描”升级为“数据库侧候选过滤、距离排序和 Top-K”，降低大数据量下的网络与 JVM 压力；保留 API、模型版本隔离和 Parent-Child 返回语义。
- **风险与回滚**：HNSW 是近似索引，召回率、构建时间和内存需要用更大数据集评测；当前只有 3 条 Qwen 候选，约 522 ms 的端到端时间主要包含远程查询 Embedding，不能作为数据库性能结论。自定义模型/维度可以使用原生 pgvector 精确排序，但若要 HNSW 需新增对应的部分索引。V5 已执行后不能修改或删除，应通过新的前向迁移演进；代码回滚前需保留兼容列。

## [VERIFY-001] 验证 Qwen Embedding 真实调用与检索闭环

- **日期**：2026-10-09
- **类型**：验证 / AI 模型 / RAG / 数据库
- **状态**：已完成
- **关联需求或问题**：用户完成 `.env` 配置后，需要确认当前项目是否真正可用，而不只是通过 Mock 契约测试。
- **验证内容**：使用项目根目录 `.env` 启动生产默认 Qwen 配置；对既有文档执行向量重建，再发起限定文档的中文语义检索；最后直接查询 PostgreSQL 核对模型版本、维度和记录数。验证过程不输出 API Key。
- **验证结果**：37 个自动化测试全部通过；应用连接 PostgreSQL 17.11 并在临时端口 `18080` 正常启动；`qwen3.7-text-embedding` 为 3 个 Child Chunk 生成 3 条 1024 维向量，重建接口耗时约 742 ms；查询“数据库连接池告警如何处置”在 3 个候选中 Top-1 命中“OpsMind 运维手册 > 告警处置”，余弦得分约 0.867，检索接口耗时约 225 ms，并返回 Parent 上下文。
- **数据核验**：文档 `79fc1fcf-93f1-44a0-bc86-95cac599f082` 同时保留 `local-feature-hashing-v1 / 256 / 3条` 与 `qwen3.7-text-embedding / 1024 / 3条`，证明不同模型索引可并存，Qwen 重建未覆盖本地基线。
- **修改效果**：FEAT-009 从“代码与契约可用”提升为“真实供应商调用、入库、召回、父上下文展开均已验证”；该结果是单样本功能验证，不等同于完整检索质量评测或并发性能结论。
- **风险与后续**：真实 API 会产生费用并受网络、限流和地域配置影响；当前延迟仅为一次小样本观测，不能写成稳定性能指标。后续使用固定问答集评测 Recall@K、MRR、P95 延迟和成本。

## [CONFIG-001] 使用项目根目录 `.env` 管理本地环境配置

- **日期**：2026-10-09
- **类型**：配置 / 安全 / 开发体验
- **状态**：已完成
- **关联需求或问题**：用户希望通过 `.env` 配置 Qwen、PostgreSQL 和 Swagger，而不是每次在 PowerShell 中逐项设置环境变量。
- **问题**：Spring Boot 默认不会自动加载 `.env`；手工执行 `$env:...` 只对当前终端会话有效，重开终端后需要重复配置，也容易漏掉模型维度或 Base URL。
- **修改内容**：
  - 在 `application.yml` 中增加 `spring.config.import=optional:file:.env[.properties]`，以 Java Properties 格式可选加载项目根目录 `.env`；文件不存在时仍可使用系统环境变量和默认值启动。
  - 新增可提交的 `.env.example`，列出数据库、Qwen Embedding 和 Swagger 所需变量，但不包含真实密钥。
  - 创建本地 `.env` 并加入 `.gitignore`；只提供空的 Key/Base URL 待用户填写，避免密钥进入 Git 历史。
  - README 改为“复制模板 → 填写 `.env` → 启动”，并说明不加引号、系统环境变量优先以及生产环境应使用密钥管理服务。
- **影响文件**：`.gitignore`、`.env.example`、本地 `.env`（不提交）、`application.yml`、`README.md`、`CHANGELOG.md`
- **验证方式**：执行完整自动化测试和 Maven 打包；使用项目根目录 `.env` 启动应用，确认 PostgreSQL/Flyway 正常连接，且缺少 Qwen 凭证时仍返回既定的安全错误而不是泄露或静默降级。
- **修改效果**：本地配置可跨终端保存，开发者只需维护一个不入库的文件；配置模板使必需变量清晰可发现，同时保持 CI/CD 环境变量覆盖能力。
- **风险与回滚**：`.env` 仍是本地明文秘密文件，只适合开发环境，应限制文件访问并避免截图、粘贴或备份泄露；生产环境使用 Vault、云 Secret Manager 或容器编排密钥。回滚时删除 `spring.config.import`、模板和忽略规则即可，现有环境变量配置仍可工作。

## [FEAT-009] 接入 Qwen 文本 Embedding 模型

- **日期**：2026-10-09
- **类型**：功能 / AI 模型 / 安全 / 配置
- **状态**：已完成；真实模型调用与检索闭环已验证
- **关联需求或问题**：用户要求项目模型使用 Qwen；将 FEAT-008 的离线词面向量基线升级为可用于中文语义检索的生产模型适配器
- **问题**：本地特征哈希只能衡量字符片段重合，无法理解同义表达和深层语义；项目虽然已有 `EmbeddingProvider` 抽象，但没有真实模型 HTTP 实现、鉴权、批处理及外部故障处理。
- **根因**：上一阶段刻意先建立无网络、可复现的检索基线，尚未引入供应商配置和远程调用边界。
- **官方依据**：阿里云百炼当前提供 OpenAI 兼容 Embedding API；`qwen3.7-text-embedding` 默认 1024 维、支持 256~2560 维，字符串数组单批最多 20 条。不同地域使用工作空间专属 Base URL 和 API Key。
- **修改内容**：
  - 新增 `QwenEmbeddingProvider`，调用 `{QWEN_BASE_URL}/embeddings`，使用 `Authorization: Bearer ${DASHSCOPE_API_KEY}`，请求包含模型名、输入数组、维度和 `encoding_format=float`。
  - 生产配置默认选择 `qwen3.7-text-embedding`、1024 维；模型、维度、Base URL、Key 和批大小全部可由环境变量覆盖，源码与配置文件不保存密钥。
  - 按配置批大小自动拆分输入，最大限制 20；按响应 `index` 恢复原输入顺序，严格校验返回条数、索引连续性和向量维度。
  - 使用条件装配隔离 Qwen 与本地 Provider：生产默认 Qwen，测试显式使用 `local-feature-hashing-v1`，避免自动化测试产生费用或依赖外网。
  - 缺少 Base URL/Key 时返回 `503 EMBEDDING_CONFIGURATION_MISSING`，不静默降级为本地向量；上游 HTTP/连接异常映射为 502，并且不把上游响应体或凭证写入用户响应。
  - README 增加 PowerShell 配置示例、地域注意事项、密钥安全要求和离线降级方法。
- **实现中发现的问题**：
  - 首个契约测试为了简化响应使用 3 维向量，触发项目已有的最小 8 维配置校验；测试改用合法 8 维夹具，生产仍使用 1024 维。
  - 完整自动化测试使用本地 Provider，因此未覆盖生产 Qwen Bean 的 Spring 装配；真实启动发现当前 WebMVC 依赖组合没有自动提供 `RestClient.Builder` Bean。改为生产构造器自行创建 `RestClient`、测试构造器注入 Mock Builder 后恢复启动。
  - 初次实现时本机没有 `DASHSCOPE_API_KEY` 和 `QWEN_BASE_URL`，因此先明确区分“HTTP 契约通过”和“真实模型待凭证验证”；用户配置 `.env` 后已在 VERIFY-001 完成真实验证。
- **影响文件**：Embedding 配置、`QwenEmbeddingProvider`、本地 Provider 条件装配、Qwen HTTP 契约测试、测试配置、`README.md`、`CHANGELOG.md`
- **验证方式**：执行 Qwen Provider 定向测试和 `.\mvnw.cmd test -q`；Mock OpenAI 兼容服务验证请求 JSON、Bearer 头、批处理、乱序响应恢复、远端 5xx 脱敏和缺少凭证拒绝；使用真实 PostgreSQL 与 `.env` 配置执行 Qwen 向量重建、语义检索和数据库核验。
- **验证结果**：37 个自动化测试通过，0 失败，0 错误；生产默认 Qwen 配置成功连接 PostgreSQL 17.11 并启动。缺少凭证时接口按设计返回 503；配置凭证后成功生成 3 条 1024 维真实 Qwen 向量，指定查询 Top-1 正确命中“告警处置”并返回 Parent 正文，详见 VERIFY-001。
- **修改前指标**：1 个本地特征哈希 Provider；生产只能生成 256 维词面基线；34 个自动化测试；没有外部模型错误边界。
- **修改后指标**：2 个可条件切换 Provider；生产默认 Qwen 1024 维；单次最多 20 条自动分批；37 个自动化测试；覆盖配置缺失、上游异常、返回校验和错误脱敏。
- **修改效果**：项目具备真实 Qwen Embedding 的生产接入路径，同时保留完全离线、无费用、可重复的测试模型；模型供应商边界与索引/搜索业务解耦，后续切换 Qwen 型号或本地 OpenAI 兼容服务无需改动业务层。
- **风险与回滚**：远程模型存在费用、限流、地域、网络和供应商可用性风险；模型或维度变化必须重建索引，不能混用不同向量空间。当前尚未实现超时/重试/熔断和 Token 成本指标，真实验证也只是单文档小样本。回滚时设置 `EMBEDDING_PROVIDER=local` 即可恢复离线基线，不涉及数据库迁移。

## [FEAT-008] 建立可复现 Embedding 与 Parent-Child 向量检索基线

- **日期**：2026-10-09
- **类型**：功能 / RAG / 检索 / 数据库
- **状态**：已完成
- **关联需求或问题**：项目实施第 8 步；把已持久化的 Child Chunk 转换为可排序向量，并验证“小块召回、大块返回”的 Parent-Child 检索路径
- **问题**：`FEAT-007` 只有分块数据，尚不能根据自然语言问题召回相关内容；若直接绑定某一家外部模型，开发、测试和演示会依赖 API Key 与网络，也难以稳定复现回归结果。
- **根因**：缺少 Embedding 模型抽象、向量版本存储、索引编排、相似度计算和搜索 API。
- **修改内容**：
  - 新增 `EmbeddingProvider` 接口，将模型名、维度和批量向量生成与索引/检索业务解耦；真实模型适配器后续可以直接替换默认实现。
  - 提供 `local-feature-hashing-v1` 本地基线：对 NFKC 归一化后的字符 1~3 gram 做 256 维带符号特征哈希并 L2 归一化，保证无网络环境下结果确定、快速且可重复测试。
  - Embedding 输入由 `headingPath + Child 正文` 组成，使章节语义参与召回；仅对 Child 建向量，命中后通过 `parentId` JOIN 返回完整 Parent 正文。
  - 新增 Flyway V4 `chunk_embedding` 表，按 `chunkId + embeddingModel` 保存模型版本、维度、序列化向量、内容哈希和生成时间；支持同一 Chunk 并存不同模型版本。
  - 向量重建在事务内按“文档 + 模型”替换，避免查询到半套索引；Chunk 或文档删除时由外键级联清理向量。
  - 新增 `POST /api/v1/documents/{documentId}/embeddings/rebuild`，要求 Child Chunk 已存在，并返回模型、维度和生成数量。
  - 新增 `POST /api/v1/search`，支持 `query`、`topK` 和可选 `documentId`；校验空查询、1000 字符上限及 Top-K 边界，按精确余弦相似度稳定排序。
  - 搜索结果同时返回 Child 命中片段、Parent 上下文、标题路径、文档信息与相似度，为后续 RAG 上下文构建和引用提供数据。
- **实现中发现的问题**：
  - 首次 Spring 上下文测试因 `DocumentEmbeddingService` 同时存在生产构造器和固定时钟测试构造器，Spring 无法选择注入入口；显式标记生产构造器为 `@Autowired` 后恢复。
  - 首次候选 JOIN 使用了不存在的 `knowledge_document.filename`；真实字段为 `original_filename`。JDBC 集成测试捕获后修正别名，避免问题进入真实 PostgreSQL。
  - 本地特征哈希并不理解同义词或深层语义，因此将其明确定位为可复现的词面向量基线，而不是虚构真实语义模型效果。
- **影响文件**：Embedding 配置与提供者接口、本地特征哈希实现、Embedding/搜索领域模型与服务、JDBC 向量仓储、文档与搜索 API、Flyway V4、配置、OpenAPI/API/JDBC/算法测试、`README.md`、`CHANGELOG.md`
- **验证方式**：执行 `.\mvnw.cmd test -q` 和 `.\mvnw.cmd package -q`；使用 H2 PostgreSQL 模式验证 V4、事务替换、向量编解码、Parent JOIN、相关性排序和接口边界；在 PostgreSQL 17.11 上对 FEAT-007 文档重建向量并发起真实中文检索，使用 SQL 核对迁移和记录数。
- **验证结果**：34 个自动化测试通过，0 失败，0 错误；真实 PostgreSQL 成功迁移到 V4。文档 `79fc1fcf-93f1-44a0-bc86-95cac599f082` 的 3 个 Child 生成 3 条 `local-feature-hashing-v1` 256 维向量；查询“数据库连接池告警如何处置”在 3 个候选中 Top-1 命中“OpsMind 运维手册 > 告警处置”，余弦得分约 0.471，并成功返回 Parent 正文。
- **修改前指标**：8 个业务 API 操作；0 条 Embedding；无法按问题检索；31 个自动化测试。
- **修改后指标**：10 个业务 API 操作；真实验证持久化 3 条 256 维向量；支持全库/单文档 Top-K 和 Parent 上下文展开；34 个自动化测试。
- **修改效果**：形成首个无需外部服务即可演示和回归的 RAG 检索闭环，为后续接入真实模型、pgvector、混合检索和质量评测提供稳定接口与可对照基线。
- **风险与回滚**：特征哈希主要衡量词面相似度，不能作为生产语义模型；向量以文本序列化且候选全部加载到 JVM，时间和内存复杂度随 Chunk 数线性增长，仅适合当前小数据基线。下一步迁移到 pgvector 数据库侧 Top-K，并用固定评测集比较真实 Embedding 与本地基线。回滚代码时可保留 V4 表；数据库降级必须通过新的前向迁移，不能编辑已执行的 V4。

## [FEAT-007] 实现标题感知的 Parent-Child 文档分块

- **日期**：2026-10-09
- **类型**：功能 / RAG / 数据库
- **状态**：已完成
- **关联需求或问题**：项目实施第 7 步；把解析后的整篇正文转换为可检索、可引用、可重复构建的 RAG 基础单元
- **问题**：`FEAT-005` 只能保存整篇正文。直接对整篇文档做 Embedding 会丢失局部语义，固定长度切割又容易破坏章节边界；同时系统缺少块级元数据、父子关系和幂等重建能力。
- **根因**：领域模型和数据库只覆盖文档与正文，尚未定义 Chunk 聚合、分块策略、块级存储和查询接口。
- **修改内容**：
  - 新增标题感知分块器，识别 Markdown 1~6 级标题并维护完整 `headingPath`；父块默认最多 1600 字符，子块最多 500 字符、重叠 50 字符，优先在段落、换行和标点边界切分。
  - 建立 Parent-Child 两级结构：子块携带 `parentId` 用于精确召回，父块保留较完整上下文用于后续生成阶段扩展。
  - 使用 `documentId + chunkType + ordinal + contentHash` 生成确定性 UUID；相同正文重复重建时 Chunk ID 保持不变。
  - 新增 Flyway V3 `document_chunk` 表，保存块类型、父块关系、序号、标题路径、正文、SHA-256、字符数、预估 Token 数和创建时间，并增加文档/类型/序号及内容哈希索引。
  - JDBC 仓储在单个事务内执行“删除旧块 + 批量写入新块”，避免查询到半套分块；数据库唯一约束防止同一文档、类型和序号重复。
  - 新增 `POST /api/v1/documents/{documentId}/chunks/rebuild` 和 `GET /api/v1/documents/{documentId}/chunks?type=CHILD&page=0&size=50`，支持按 PARENT/CHILD 分页查看，并补充 Swagger/OpenAPI 契约。
  - 分块参数通过 `opsmind.chunking` 配置，可在不修改代码的情况下调整父块、子块和重叠窗口大小。
- **实现中发现的问题**：
  - 初版遇到“一级标题后立即出现二级标题”时，会把只有标题、没有正文的章节保存成无价值 Chunk；增加标题-only 判断后跳过该段，并由后续子标题继承层级路径。
  - 首个重叠分块测试样本过短，每个父块只产生一个子块，无法真正验证重叠窗口；扩大测试段落后覆盖多子块场景及相邻子块的重叠内容。
  - 真实接口验证脚本最初把统一响应体的业务字段误读为顶层字段；修正为读取 `data.documentId` 和 `data.items` 后完成全链路验证，业务接口本身无异常。
- **影响文件**：分块配置、Chunk 领域模型、分块器与编排服务、JDBC/内存仓储、文档 API 与响应 DTO、Flyway V3、OpenAPI/API/JDBC/单元测试、`README.md`、`CHANGELOG.md`
- **验证方式**：执行 `.\mvnw.cmd test -q` 和 `.\mvnw.cmd package -q`；使用 H2 PostgreSQL 模式验证 V3、自引用外键、事务替换和分页；在 PostgreSQL 17.11 上上传 Markdown，依次解析、重建、查询并再次重建，最后使用 SQL 核对迁移和记录数。
- **验证结果**：31 个自动化测试通过，0 失败，0 错误；真实 PostgreSQL 成功迁移到 V3。验证文档 `79fc1fcf-93f1-44a0-bc86-95cac599f082` 状态为 `READY`，生成 3 个 Parent 和 3 个 Child；首个标题路径为“OpsMind 运维手册 > 告警处置”，连续两次重建的全部 Child ID 一致。
- **修改前指标**：6 个业务 API 操作；0 个持久化 Chunk；25 个自动化测试；正文只能整篇读取。
- **修改后指标**：8 个业务 API 操作；单次验证持久化 6 个 Chunk；31 个自动化测试；支持 PARENT/CHILD 分页与确定性重建。
- **修改效果**：RAG 离线链路从“可解析正文”推进到“可检索知识单元”，既保留章节语义，又为后续小块召回、大块扩展、引用定位和增量 Embedding 提供稳定数据模型。
- **风险与回滚**：当前按字符预算并用 `ceil(characterCount / 4)` 估算 Token，并非模型精确 tokenizer；标题识别主要面向 Markdown，部分 PDF/DOCX 的结构信息可能在 Tika 纯文本阶段丢失；同步重建不适合超大文档。回滚代码时保留 V3 表不会影响旧版本读取，若要删除表必须先备份并通过新的前向迁移处理，不能修改已执行的 V3。

## [FEAT-006] 增加数据库文档分页列表

- **日期**：2026-10-09
- **类型**：功能 / API / 数据库查询
- **状态**：已完成
- **关联需求或问题**：需要查看当前数据库中已有的文档及其处理状态
- **问题**：已有接口只能在已知文档 ID 时查询单条记录，Swagger 和后续管理页面无法发现数据库中有哪些文档，也无法查看总量和批量处理状态。
- **根因**：文档仓储仅提供按 ID/checksum 查询与写入方法，没有面向列表场景的查询模型和分页 API。
- **修改内容**：
  - 新增 `GET /api/v1/documents?page=0&size=20`，页码从 0 开始，默认 20 条、最大 100 条。
  - 返回 `items、page、size、totalElements、totalPages`，列表项包含文件信息、SHA-256、文档状态、任务状态、任务错误和创建时间。
  - 新增独立查询仓储，使用 `knowledge_document LEFT JOIN ingestion_task` 一次取得文档与任务，避免列表场景的 N+1 查询。
  - 按 `created_at DESC, id DESC` 排序，在相同创建时间下仍保持稳定顺序；使用 LEFT JOIN 避免文档和任务创建之间的短暂窗口漏数。
  - 增加页码和页大小边界检查，非法参数返回统一错误码 `DOCUMENT_PAGE_INVALID` 或 `DOCUMENT_PAGE_SIZE_INVALID`。
  - 为列表接口补充 Swagger/OpenAPI 描述及分页响应模型。
- **实现中发现的问题**：若直接复用单文档服务逐项查询任务，每页会产生 `1 + N` 次 SQL；本次通过专用 JOIN 查询模型将列表数据查询固定为 2 次 SQL（1 次分页明细、1 次总数），不随页内文档数量增加。
- **影响文件**：文档查询领域模型、`DocumentCatalogService`、`JdbcDocumentQueryRepository`、列表响应 DTO、`DocumentController`、OpenAPI/API/JDBC/服务测试、`README.md`、`CHANGELOG.md`
- **验证方式**：执行 `.\mvnw.cmd test -q`；使用 H2 PostgreSQL 模式验证稳定倒序、任务 JOIN、分页统计、边界拒绝及 OpenAPI；在 PostgreSQL 17.11 上请求真实列表接口。
- **验证结果**：25 个自动化测试通过，0 失败，0 错误；真实数据库返回 4 个文档、1 页，最新 `feat005.txt` 状态为 `READY / SUCCEEDED`；OpenAPI 包含 `/api/v1/documents` 的 GET 操作。
- **修改前指标**：5 个业务 API 操作；无法枚举数据库文档；21 个自动化测试；逐条复用现有仓储会产生 N+1 查询。
- **修改后指标**：6 个业务 API 操作；支持最多 100 条/页的分页列表；25 个自动化测试；每页固定 2 次 SQL 查询。
- **修改效果**：可以直接通过 Swagger 或管理页面发现数据库中的全部文档、查看处理状态并分页浏览，为后续重试、删除、筛选和批量操作提供入口。
- **风险与回滚**：`OFFSET` 分页在极大页码和高并发写入下性能及一致性有限；当前个人项目数据规模适用，后续数据量增大时可改为基于 `created_at + id` 的游标分页。回滚时删除查询服务、查询仓储和 GET 路由即可，不涉及数据库结构或数据变更。

## [FIX-002] 恢复不可变的 Flyway V1 迁移文件

- **日期**：2026-10-09
- **类型**：修复 / 数据库
- **状态**：已完成
- **关联需求或问题**：应用启动时报 `Migration checksum mismatch for migration version 1`
- **问题**：Flyway 校验发现数据库记录的 V1 checksum 为 `-1887772990`，本地解析结果为 `-1378981519`，因此拒绝启动应用。
- **根因**：已经执行并提交的 `V1__create_document_tables.sql` 在第一个建表语句后被额外插入了 `-0`，改变了迁移文件内容和 checksum。数据库结构及迁移历史本身没有损坏。
- **修改内容**：删除误插入的 `-0`，将 V1 精确恢复为 Git 提交 `481af61` 中已执行过的版本；没有执行 `flyway repair`，也没有修改数据库迁移历史或删除数据。
- **影响文件**：`V1__create_document_tables.sql`、`CHANGELOG.md`
- **验证方式**：比较工作区与 Git 中的 V1；只读查询 `flyway_schema_history` 和三张业务表；执行应用启动或完整测试，确认 Flyway 能验证 V1/V2。
- **验证结果**：数据库中的 V1、V2 均为成功状态，`knowledge_document`、`ingestion_task`、`document_content` 均存在；V1 恢复后，真实 PostgreSQL 17.11 输出 `Successfully validated 2 migrations`，应用在 `18080` 正常启动。
- **修改前指标**：应用启动失败；本地 V1 checksum `-1378981519` 与数据库 `-1887772990` 不一致。
- **修改后指标**：本地 V1 与 Git 提交版本完全一致；Flyway 2 个迁移全部校验通过；应用启动成功；数据库历史和业务数据 0 项修改。
- **修改效果**：恢复 Flyway 迁移文件不可变性，避免用 `repair` 掩盖本地脚本被误改的问题。
- **风险与回滚**：本次仅恢复误改字符，不改变任何数据库对象和数据。不要再次编辑已执行的 V1/V2；后续结构变化必须新增 V3、V4 等迁移文件。

## [FEAT-005] 实现文档解析、文本清洗与可恢复处理任务

- **日期**：2026-10-09
- **类型**：功能 / 数据 / 稳定性
- **状态**：已完成
- **关联需求或问题**：项目实施第 5 步；为 RAG 分块和 Embedding 提供可追踪的标准化正文
- **问题**：系统只能保存原始文件和任务元数据，无法从 PDF、DOCX、HTML、Markdown、TXT 中抽取统一文本；任务也没有实际状态流转、失败原因、重试或进程中断恢复能力。
- **根因**：`FEAT-004` 仅建立上传与持久化入口，尚未实现解析器、正文模型、处理编排和状态更新仓储操作。
- **修改内容**：
  - 按 Apache Tika 官方依赖方式引入 `tika-core` 与 `tika-parsers-standard-package` 3.3.2，使用 `AutoDetectParser` 抽取正文、标题和检测媒体类型。
  - 新增文本清洗器，执行 Unicode NFC、换行标准化、非法控制字符移除、行尾空白清理及连续空行压缩。
  - 新增 `POST /api/v1/documents/{documentId}/process` 和 `GET /api/v1/documents/{documentId}/content`，均生成 Swagger 文档。
  - 新增 Flyway V2 `document_content` 表，持久化正文、标题、检测媒体类型、字符数和解析时间，并以文档 ID 保证幂等覆盖。
  - 增加 `UPLOADED → PARSING → READY/FAILED` 与 `PENDING → RUNNING → SUCCEEDED/FAILED` 状态流转，失败时保存不超过 2000 字符的安全错误信息。
  - 使用条件更新抢占任务；活动租约内拒绝并发处理，`RUNNING` 超过 5 分钟可被重新抢占；`FAILED` 可直接重试，`SUCCEEDED` 重复调用幂等返回已有正文。
  - 配置最大抽取文本为 2,000,000 字符，区分文本超限和普通解析失败。
- **实现中发现的问题**：
  - 首次编译时编排层和 Tika 适配器都尝试关闭同一个输入流，编排层出现未处理 `IOException`；最终统一为存储层打开、编排层关闭、解析器只读取。
  - 为兼容既有测试保留双参数配置构造器后，Spring Boot 无法选择配置绑定构造器，应用上下文启动失败；显式标记规范构造器为 `@ConstructorBinding` 后恢复。
  - 初版将所有 `SAXException` 都归类为文本超限，可能掩盖损坏文档；改用 `WriteLimitReachedException.isWriteLimitReached` 精确识别，其他异常返回普通解析失败。
  - 单例共享 `AutoDetectParser` 的并发安全边界不够明确；改为每次解析创建独立解析器实例。
- **影响文件**：`pom.xml`、文档领域与仓储接口、解析/清洗/编排服务、JDBC 与内存适配器、文档 API、Flyway V2、应用配置、测试及 `README.md`
- **验证方式**：
  - 执行 `.\mvnw.cmd test -q`，覆盖文本清洗、HTML/TXT 抽取、空正文失败、失败重试、活动租约拒绝、过期租约回收、文本上限、V2 迁移、HTTP 全链路和 OpenAPI 契约。
  - 在 PostgreSQL 17.11 上启动应用，上传包含连续空行和中文内容的 TXT，调用处理和正文接口，再次调用处理接口验证幂等，并使用 SQL 核对状态与 V2 迁移。
- **验证结果**：21 个自动化测试通过，0 失败，0 错误；H2 PostgreSQL 模式与真实 PostgreSQL 均成功迁移到 V2。真实验证文档 ID 为 `d672686f-fe16-4900-b2f3-21b7ef2a9836`，连续空行被压缩，保存正文 49 字符，检测类型为 `text/plain; charset=UTF-8`，文档状态 `READY`、任务状态 `SUCCEEDED`；重复处理保持成功状态和同一正文。
- **修改前指标**：15 个自动化测试；0 条持久化解析正文；任务创建后长期停留在 `PENDING`；3 个业务 API 操作；可执行 JAR 35,906,369 字节。
- **修改后指标**：21 个自动化测试；单文档最多保存 1 条幂等正文；完整成功/失败/重试/租约恢复状态流转；5 个业务 API 操作；可执行 JAR 92,377,476 字节。
- **修改效果**：形成可通过 Swagger 演示的文档 ETL 闭环，解析正文具备持久化、查询、失败诊断和中断恢复基础，可直接作为后续分块输入。
- **风险与回滚**：解析仍在同步 HTTP 线程执行，大文件可能增加延迟和内存使用；Tika 标准解析器使 JAR 增加约 56.5 MB，且在应用进程内运行，尚未做进程级资源隔离；正文保存与最终状态更新不是单一事务，但重试会幂等覆盖正文。回滚代码后不要自动删除 V2 表；如需清理应先备份，再显式删除 `document_content` 并修复 Flyway 历史，或恢复迁移前备份。

## [ARCH-002] 将 PostgreSQL 数据目录绑定到当前项目

- **日期**：2026-10-09
- **类型**：架构 / 配置
- **状态**：已完成
- **关联需求或问题**：开发者希望在当前项目中定位 PostgreSQL 持久化数据
- **问题**：Docker 命名卷存放在 Docker Desktop 的 Linux 虚拟机中，Windows 项目目录不可直接看到数据库文件，也不便于理解数据的持久化位置。
- **根因**：`compose.yml` 使用 `opsmind-postgres-data` 命名卷，实际宿主路径由 Docker 管理。
- **修改内容**：
  - 将 PostgreSQL 挂载改为 `./data/postgres:/var/lib/postgresql/data`。
  - 数据库切换前使用 `pg_dump` 生成 `data/backups/opsmind-before-bind-mount.dump` 逻辑备份。
  - 重建容器并将备份恢复到新的绑定目录；原命名卷 `opsmind_opsmind-postgres-data` 保留，作为额外回滚副本。
  - 保持 `data/` 在 `.gitignore` 中，防止数据库二进制文件、上传内容和备份被提交到 Git。
  - 在 README 中补充数据位置和 `psql` 查看方式。
- **影响文件**：`compose.yml`、`README.md`、`CHANGELOG.md`；运行时新增 `data/postgres` 和 `data/backups`（均不提交）
- **验证方式**：使用 `docker inspect opsmind-postgres-1` 检查挂载类型和来源；恢复备份后使用 `psql` 查询文档、任务及 Flyway 迁移记录数量。
- **验证结果**：挂载类型为 `bind`，来源为项目的 `data/postgres`；容器正常接受连接；恢复后包含 2 条文档、2 条任务和 1 条成功迁移记录。
- **修改前指标**：数据库位于 Docker 命名卷，项目目录中不可见；已有数据为 2 条文档和 2 条任务。
- **修改后指标**：数据库位于项目 `data/postgres`；迁移后仍为 2 条文档和 2 条任务，记录数量无损失。
- **修改效果**：开发者可以在项目目录中明确定位数据库文件，并继续通过 `psql` 或数据库客户端查看逻辑数据。
- **风险与回滚**：PostgreSQL 物理文件不可直接编辑、复制中的数据库必须先停止，项目目录也会显著增大。回滚时停止容器，将 Compose 挂载改回命名卷后重建；原命名卷未删除，可直接重新挂载。逻辑备份也可通过 `pg_restore` 恢复。

## [FEAT-004] 使用 PostgreSQL 与 Flyway 持久化文档元数据

- **日期**：2026-10-09
- **类型**：功能 / 架构 / 数据
- **状态**：已完成
- **关联需求或问题**：项目实施第 4 步；解决应用重启后元数据丢失和多实例无法可靠去重的问题
- **问题**：文档和入库任务只保存在进程内存中，应用重启后无法查询历史文档；多个应用实例也无法共享去重索引，相同内容可能被重复创建。
- **根因**：`FEAT-002` 使用 `ConcurrentHashMap` 作为临时仓储适配器，没有外部持久化存储和数据库级唯一约束。
- **修改内容**：
  - 引入 Spring JDBC、PostgreSQL 驱动、Flyway PostgreSQL 扩展以及测试用 H2。
  - 新增 `knowledge_document` 和 `ingestion_task` 表，通过文档 SHA-256 唯一约束和任务 `document_id` 唯一约束保证原子去重。
  - 新增 JDBC 仓储适配器，捕获并发插入的唯一键冲突后读取既有记录，使重复请求返回同一文档和任务。
  - 新增 Flyway V1 迁移，应用启动时自动创建并校验数据库结构。
  - 新增 Docker Compose PostgreSQL 17 开发环境、持久化卷和健康检查；宿主机端口使用 `5433`，避免占用已有的 `5432` 服务。
  - 保留内存仓储作为纯单元测试替身，但不再注册为生产 Spring Bean。
- **实现中发现的问题**：
  - 本机 `5432` 已被其他 PostgreSQL 容器占用，Compose 首次启动端口绑定失败；改为宿主机 `5433`，未中断或修改已有服务。
  - 本机 `8080` 已被其他进程占用；真实数据库验证临时使用 `18080`，未终止未知进程。
  - 测试专用 `application.yml` 会覆盖而不是局部合并主配置，最初缺少应用名导致占位符解析失败；补齐测试应用名和数据源配置后恢复。
  - Flyway 历史表同时包含 schema 创建标记和 V1 迁移，最初断言总行数会误判；测试改为精确检查版本 `1`。
- **影响文件**：`pom.xml`、`compose.yml`、`application.yml`、Flyway V1 迁移、JDBC/内存仓储适配器、测试数据源配置、JDBC 仓储测试、`README.md`
- **验证方式**：
  - 执行 `.\mvnw.cmd test -q`，使用 H2 PostgreSQL 模式验证 Flyway 迁移、文档持久化、SHA-256 去重和任务唯一性。
  - 启动 PostgreSQL 17.11 容器与应用，在 `18080` 连续两次上传同一份 `README.md`，查询数据库记录数，然后重启应用并按原文档 ID 查询。
- **验证结果**：15 个自动化测试通过，0 失败，0 错误；真实 PostgreSQL 成功应用 V1 迁移。同一文件两次上传返回相同文档 ID `78693979-d4b1-4755-80b1-40f771703294` 和任务 ID `34e9317b-9dcd-4e7d-96ba-5ef732c1d915`，第二次响应 `duplicate=true`；数据库中保持 1 条文档和 1 条任务；应用重启后仍能查询该文档。
- **修改前指标**：元数据重启后丢失；去重范围仅限单进程生命周期；13 个自动化测试。
- **修改后指标**：元数据跨应用重启保留；数据库唯一约束提供跨实例原子去重；同内容 2 次上传仅产生 1 条文档和 1 条任务；15 个自动化测试。
- **修改效果**：文档接入链路具备可恢复、可共享的元数据基础，Flyway 让数据库结构随代码版本演进，后续解析任务可以从持久化状态继续推进。
- **风险与回滚**：文件系统写入与数据库写入尚不具备跨资源事务，数据库失败时可能残留孤立文件；后续通过补偿清理和可恢复任务机制解决。回滚时先停止应用，回退 JDBC/Flyway 代码和配置；数据库迁移不执行自动降级，如需清理应先备份再显式删除 V1 表。开发环境可执行 `docker compose -p opsmind stop postgres` 停止数据库，数据卷默认保留。

## [FEAT-003] 集成 Swagger UI 与 OpenAPI 接口文档

- **日期**：2026-10-09
- **类型**：功能 / 文档
- **状态**：已完成
- **关联需求或问题**：缺少浏览器端后端调试界面
- **问题**：现有接口只能通过 curl 或测试代码调用，无法在浏览器中直观看到请求参数、响应模型、状态码，也不能方便地选择文件测试上传。
- **根因**：项目未集成 OpenAPI 文档生成器和 Swagger UI，Controller 也没有接口语义注解。
- **修改内容**：
  - 引入适配 Spring Boot 4 的 `springdoc-openapi-starter-webmvc-ui` 3.1.0。
  - 新增 OpsMind OpenAPI 元信息，为系统和知识文档接口补充 Tag、Operation、参数、响应码及字段示例。
  - 配置 Swagger UI 请求耗时、排序和 Try it out 功能。
  - 提供 `/swagger-ui/index.html` 调试界面和 `/v3/api-docs` OpenAPI JSON。
  - 支持通过环境变量 `SWAGGER_ENABLED=false` 在生产环境关闭文档端点。
- **实现中发现的问题**：首次 OpenAPI 测试发现上传接口被错误描述为 `application/json`，Swagger UI 不会生成文件选择控件。根因是 `@PostMapping` 未显式声明消费类型；增加 `consumes = multipart/form-data` 后文档契约恢复正确。
- **影响文件**：`pom.xml`、`OpenApiConfiguration.java`、`SystemController.java`、`DocumentController.java`、`DocumentResponse.java`、`application.yml`、OpenAPI 测试、`README.md`
- **验证方式**：执行 `.\mvnw.cmd test -q`；自动请求 `/v3/api-docs` 和 `/swagger-ui/index.html`，检查文档元信息、3 个业务操作以及上传请求的 `multipart/form-data` 描述。
- **验证结果**：13 个测试通过，0 失败，0 错误；Swagger UI HTML 返回 200，OpenAPI JSON 返回 200 且包含全部业务接口。
- **修改前指标**：无浏览器调试界面；无机器可读 API 文档；11 个自动化测试。
- **修改后指标**：1 个 Swagger UI、1 个 OpenAPI JSON 端点；3 个业务操作生成文档；13 个自动化测试。
- **修改效果**：开发者可以直接在浏览器中查看并调试接口，文档上传操作能够显示文件选择控件，接口说明与代码同步生成。
- **风险与回滚**：Swagger 会增加少量启动时间和依赖体积，且接口文档不应在公开生产环境直接暴露；可设置 `SWAGGER_ENABLED=false` 关闭。回滚时移除 Springdoc 依赖、配置类和 Swagger 注解，不影响业务接口。

## [FEAT-002] 实现文档上传、校验、内容去重与任务状态

- **日期**：2026-10-09
- **类型**：功能 / 安全
- **状态**：已完成
- **关联需求或问题**：项目实施第 2 步
- **问题**：系统缺少知识文档入口，无法接收和保存 RAG 数据源；也没有入库任务标识、状态模型或重复内容识别能力。
- **根因**：`FEAT-001` 只提供了可运行的 Web 工程骨架，没有知识库领域模型和文档接入链路。
- **修改内容**：
  - 新增 `POST /api/v1/documents` 和 `GET /api/v1/documents/{documentId}`。
  - 支持 PDF、DOCX、Markdown、TXT、HTML，默认单文件上限 20 MB。
  - 清理浏览器伪路径和控制字符，服务端根据扩展名确定标准媒体类型。
  - 校验 PDF 魔数、DOCX 必需的 OOXML ZIP 条目，并拒绝包含二进制空字节的文本文件。
  - 对文件内容计算 SHA-256，以 `checksum.extension` 内容寻址落盘；重复内容返回既有文档和任务。
  - 建立 `KnowledgeDocument`、`IngestionTask` 及其状态枚举和仓储接口，使用线程安全的内存实现作为首版适配器。
  - 增加统一文档异常响应，包括空文件、超限、不支持类型、签名不匹配和文档不存在。
  - 处理并发写入：仓储原子保存 checksum 与 ID 索引，本地文件采用临时文件和原子移动，容忍相同内容并发上传。
- **实现中发现的问题**：首次上下文测试因 `DocumentService` 存在生产和测试两个构造函数，Spring 无法确定注入目标；为生产构造函数增加显式 `@Autowired` 后恢复启动，同时保留固定时钟测试能力。
- **影响文件**：`src/main/java/com/opsmind/knowledge/document/`、`GlobalExceptionHandler.java`、`ApiResponse.java`、`application.yml`、`.gitignore`、对应测试及 `README.md`
- **验证方式**：在 Java 21.0.10 环境执行 `.\mvnw.cmd test -q`，覆盖应用启动、API 上传、重复内容、非法扩展名、伪造 PDF、超限文件、路径清理、状态查询和不存在文档。
- **验证结果**：11 个测试通过，0 失败，0 错误；其中新增文档相关测试 9 个。
- **修改前指标**：1 个业务接口；2 个自动化测试；不支持文档上传、内容去重和任务状态。
- **修改后指标**：3 个业务接口；11 个自动化测试；同一内容连续上传只生成 1 个原始文件、1 个文档 ID 和 1 个任务 ID。
- **修改效果**：形成 RAG 离线链路的可靠入口，能够在解析前拒绝常见非法输入，并避免相同内容重复占用存储或重复创建任务。
- **风险与回滚**：文档和任务元数据当前只保存在单实例内存中，重启后丢失且不支持多实例全局去重；原始文件会保留。后续使用 PostgreSQL/Flyway 替换内存适配器。回滚时删除知识文档模块并移除相关配置，`FEAT-001` 系统接口不受影响。

## [FIX-001] 修复 Maven Wrapper 在当前 PowerShell 环境无法启动

- **日期**：2026-10-08
- **类型**：修复
- **状态**：已完成
- **关联需求或问题**：`FEAT-001` 首次构建验证
- **问题**：执行 `.\mvnw.cmd test` 时出现 `Cannot index into a null array`，Wrapper 在下载或启动 Maven 前直接退出。
- **根因**：官方 Wrapper 脚本直接访问 `(Get-Item $MAVEN_M2_PATH).Target[0]`；当前用户的 `.m2` 是普通目录，`Target` 为 `null`，数组索引操作抛出异常。
- **修改内容**：先保存目录的 `Target`，增加空值判断，仅在确实为符号链接时访问第一个目标。
- **影响文件**：`mvnw.cmd`
- **验证方式**：执行 `.\mvnw.cmd test`。
- **验证结果**：Wrapper 成功下载并启动 Maven 3.9.16，项目构建成功。
- **修改前指标**：构建在 Wrapper 启动阶段失败，测试无法运行。
- **修改后指标**：测试执行成功，2 个测试通过，0 失败，0 错误。
- **修改效果**：Windows 当前 PowerShell 环境可直接使用项目 Wrapper 构建，无需全局安装 Maven。
- **风险与回滚**：仅改变普通目录/符号链接判断；可用 Spring Initializr 原始 `mvnw.cmd` 覆盖回滚，但会恢复本机启动失败。

## [FEAT-001] 建立可运行的 Spring Boot 工程骨架

- **日期**：2026-10-08
- **类型**：功能
- **状态**：已完成
- **关联需求或问题**：项目实施第 1 步
- **问题**：项目只有方案文档，缺少可编译、可测试、可运行的代码基线，后续 RAG 与 Agent 能力无处承载。
- **根因**：项目处于设计阶段，工作区没有 Java 工程和构建工具配置。
- **修改内容**：
  - 使用 Java 21、Spring Boot 4.1.1 和 Maven Wrapper 建立工程。
  - 引入 Web MVC、Validation、Actuator，并导入 Spring AI 2.0.1 BOM。
  - 建立 `common.api`、`system` 包边界和统一 `ApiResponse`。
  - 实现 `GET /api/v1/system/info`，配置健康端点、优雅停机和构建信息。
  - 添加应用上下文测试与 Web MVC 接口测试。
  - 初始化本地 Git 仓库与 `main` 分支，为后续代码级变更追踪建立基础。
- **影响文件**：`pom.xml`、`.mvn/`、`mvnw`、`mvnw.cmd`、`src/main/`、`src/test/`、`README.md`
- **验证方式**：在 Java 21.0.10 环境执行 `.\mvnw.cmd test` 和 `.\mvnw.cmd package -q`。
- **验证结果**：`BUILD SUCCESS`；2 个测试通过，0 失败，0 错误；成功生成可运行的 `opsmind-0.0.1-SNAPSHOT.jar`（23,645,845 字节）。首次测试的 Maven 统计耗时为 11.745 秒。
- **修改前指标**：0 个可执行接口，0 个自动化测试，项目不可构建。
- **修改后指标**：1 个业务探针接口，2 个自动化测试，构建成功率 100%（本次 1/1）。
- **修改效果**：形成可持续迭代的应用基线，并验证 Spring Boot 4.1.1、Spring AI 2.0.1 BOM 与 Java 21 可解析和编译。
- **风险与回滚**：尚未接入数据库和外部服务，运行风险低；删除工程文件可退回文档设计阶段。

## [ARCH-001] 建立项目方案与可追踪变更机制

- **日期**：2026-10-08
- **类型**：架构 / 工程规范
- **状态**：已完成
- **问题**：项目尚无统一设计文档，也没有机制说明每次开发针对什么问题、修改了什么以及产生了什么效果，后续难以复盘和形成可信的简历素材。
- **根因**：工作区为空，项目仍处于方案设计阶段，尚未建立工程和文档规范。
- **修改内容**：
  - 新增企业知识与工单处置 Agent 的完整项目设计。
  - 新增统一的变更编号、记录字段和 Git 提交约定。
  - 新增变更记录模板，要求保存验证方法、前后指标及回滚方案。
- **影响文件**：
  - `README.md`
  - `CHANGELOG.md`
  - `docs/CHANGE_RECORD_GUIDE.md`
- **验证方式**：检查上述文档存在，且模板包含问题、修改、验证、效果和回滚字段。
- **验证结果**：文档结构已建立；尚无业务代码或性能指标可验证。
- **修改效果**：后续开发可以按统一格式形成可审查、可复盘的证据链；实际执行效果将在后续变更中持续验证。
- **风险与回滚**：仅新增文档规范，无运行时风险；删除新增章节与文档即可回滚。

---

## 记录模板

复制以下内容，并将新记录放在本文件顶部：

```markdown
## [类型-编号] 简短标题

- **日期**：YYYY-MM-DD
- **类型**：功能 / 修复 / 性能 / 架构 / 安全 / 文档
- **状态**：计划中 / 进行中 / 已完成 / 已回滚
- **关联需求或问题**：Issue、任务或评测用例编号；没有则写“无”
- **问题**：用户现象、业务需求或技术问题，以及能够证明问题存在的证据
- **根因**：问题的直接原因；新功能填写当前能力缺口
- **修改内容**：具体实现与关键设计选择
- **影响文件**：关键文件或模块列表
- **验证方式**：测试、评测、压测命令及测试数据
- **验证结果**：通过/失败情况和关键输出
- **修改前指标**：同一数据集、环境及统计口径下的数据；没有则写“未建立基线”
- **修改后指标**：实测数据；尚未测量则写“待验证”，不能填写预估值
- **修改效果**：解决了什么、尚未解决什么
- **风险与回滚**：兼容性、潜在副作用和明确回滚步骤
```
