# Repository Guidelines

## 项目结构与模块划分

本项目是基于 Java 21、Spring Boot 4 的 RAG Agent 应用。生产代码位于 `src/main/java/com/opsmind`，按业务模块划分为 `agent`、`rag`、`knowledge/document`、`evaluation` 和 `system`，并继续分为以下层次：

- `api`：接收 HTTP 请求、校验参数和转换响应 DTO。
- `application`：编排业务流程，不直接依赖具体数据库或模型厂商。
- `domain`：定义领域对象、状态枚举和 Repository 接口。
- `infrastructure`：实现 JDBC、文件存储以及 Qwen 模型调用。
- `config`：绑定 `application.yml` 和环境变量。

配置与 Flyway 脚本位于 `src/main/resources`，测试代码位于 `src/test/java`，监控配置位于 `deploy/monitoring`，评测数据和报告位于 `evaluation`。运行数据只能保存在已忽略的 `data/` 目录。

## 构建、测试与本地运行

Windows 环境统一使用项目自带的 Maven Wrapper：

- `docker compose -p opsmind up -d`：启动 PostgreSQL、Prometheus、Grafana 和 Alertmanager。
- `.\mvnw.cmd spring-boot:run`：启动后端，默认监听 8080 端口。
- `.\mvnw.cmd test`：运行完整自动化测试。
- `.\mvnw.cmd package -DskipTests`：生成 `target/opsmind-0.0.1-SNAPSHOT.jar`。
- `docker compose config`：检查 Compose 配置是否合法。

Swagger 地址为 `http://localhost:8080/swagger-ui/index.html`。

## 编码风格与命名规范

统一使用 UTF-8、四空格缩进、构造器注入和不可变 `record` DTO。类和接口使用 `PascalCase`，方法与字段使用 `camelCase`，常量使用 `UPPER_SNAKE_CASE`。接口名称描述能力，例如 `EmbeddingProvider`；实现类名称体现技术方案，例如 `QwenEmbeddingProvider`、`JdbcDocumentRepository`。

保留现有面向初学者的中文同行注释。注释应解释设计原因、输入输出和边界条件，不要只重复代码字面含义。

## 测试要求

测试使用 JUnit 5、AssertJ、Mockito、MockMvc 和 H2。测试类以 `*Tests` 结尾，方法名应描述可观察行为。算法和服务分支使用单元测试；HTTP 契约或数据库行为变化时增加 Controller 或 Repository 测试。每个缺陷修复都应补充回归测试，提交前运行 `.\mvnw.cmd test`。

## Commit 与 Pull Request 规范

提交信息遵循现有格式，例如：

- `feat(agent): FEAT-024 增加权限过滤`
- `fix(rag): 修复引用校验`
- `docs: DOC-004 更新开发指南`

每个提交只处理一个主题。重要修改需要在 `CHANGELOG.md` 中记录问题、修改、效果、验证结果和限制。Pull Request 应说明行为变化、测试命令与结果、关联需求，以及数据库迁移和配置变化；涉及 Swagger 或 Grafana 展示时附截图。

## 安全与配置

从 `.env.example` 复制生成 `.env`。严禁提交真实密钥、`.env` 或 `data/` 下的运行数据。日志、异常和审计信息中不得记录 API Key、完整 Prompt 或敏感文档正文。检索内容始终按不可信输入处理，写工具必须保留人工审批和风险校验。
