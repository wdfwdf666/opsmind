package com.opsmind.agent.config; // 包作用：配置层，把外部配置转换成 Spring 可管理的对象。

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.agent.planner") // 注解作用：把指定前缀的 YAML 或环境变量绑定到当前对象。
public record AgentPlannerProperties(
        String provider /* 字段作用：当前选择的能力提供方式，例如 qwen 或 local。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        String baseUrl /* 字段作用：该不可变对象携带的 baseUrl 数据。 */,
        String apiKey /* 字段作用：该不可变对象携带的 apiKey 数据。 */,
        int maxOutputTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        int connectTimeoutMs /* 字段作用：该不可变对象携带的 connectTimeoutMs 数据。 */,
        int readTimeoutMs /* 字段作用：该不可变对象携带的 readTimeoutMs 数据。 */,
        int maxAttempts /* 字段作用：该不可变对象携带的 maxAttempts 数据。 */,
        int retryInitialBackoffMs /* 字段作用：该不可变对象携带的 retryInitialBackoffMs 数据。 */,
        int retryMaxBackoffMs /* 字段作用：该不可变对象携带的 retryMaxBackoffMs 数据。 */,
        int circuitFailureThreshold /* 字段作用：该不可变对象携带的 circuitFailureThreshold 数据。 */,
        int circuitOpenDurationMs /* 字段作用：该不可变对象携带的 circuitOpenDurationMs 数据。 */,
        boolean fallbackEnabled /* 字段作用：该不可变对象携带的 fallbackEnabled 数据。 */,
        String fallbackModel /* 字段作用：该不可变对象携带的 fallbackModel 数据。 */) { // 类型作用：配置对象，字段值来自 application.yml 或 .env。

    public AgentPlannerProperties { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        if (provider == null || (!provider.equals("qwen") && !provider.equals("local"))) {
            throw new IllegalArgumentException("Agent planner provider must be qwen or local");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Agent planner model must not be blank");
        }
        if (maxOutputTokens < 32 || maxOutputTokens > 1000) {
            throw new IllegalArgumentException("Agent planner max output tokens must be between 32 and 1000");
        }
        if (connectTimeoutMs < 100 || connectTimeoutMs > 30000
                || readTimeoutMs < 100 || readTimeoutMs > 120000) {
            throw new IllegalArgumentException("Agent planner timeouts are invalid");
        }
        if (maxAttempts < 1 || maxAttempts > 3) {
            throw new IllegalArgumentException("Agent planner max attempts must be between 1 and 3");
        }
        if (retryInitialBackoffMs < 0 || retryInitialBackoffMs > 10000
                || retryMaxBackoffMs < retryInitialBackoffMs || retryMaxBackoffMs > 60000) {
            throw new IllegalArgumentException("Agent planner retry backoff is invalid");
        }
        if (circuitFailureThreshold < 1 || circuitFailureThreshold > 100
                || circuitOpenDurationMs < 100 || circuitOpenDurationMs > 600000) {
            throw new IllegalArgumentException("Agent planner circuit breaker configuration is invalid");
        }
        if (fallbackEnabled && (fallbackModel == null || fallbackModel.isBlank())) {
            throw new IllegalArgumentException("Agent planner fallback model must not be blank");
        }
    }

}
