package com.opsmind.knowledge.document.config; // 包作用：配置层，把外部配置转换成 Spring 可管理的对象。

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.reranking") // 注解作用：把指定前缀的 YAML 或环境变量绑定到当前对象。
public record RerankingProperties(
        String provider /* 字段作用：当前选择的能力提供方式，例如 qwen 或 local。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        String endpoint /* 字段作用：该不可变对象携带的 endpoint 数据。 */,
        String apiKey /* 字段作用：该不可变对象携带的 apiKey 数据。 */,
        String instruction /* 字段作用：该不可变对象携带的 instruction 数据。 */,
        int candidateMultiplier /* 字段作用：该不可变对象携带的 candidateMultiplier 数据。 */,
        int minimumCandidates /* 字段作用：该不可变对象携带的 minimumCandidates 数据。 */) { // 类型作用：配置对象，字段值来自 application.yml 或 .env。

    public RerankingProperties { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        if (provider == null || (!provider.equals("qwen") && !provider.equals("local"))) {
            throw new IllegalArgumentException("Reranking provider must be qwen or local");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Reranking model must not be blank");
        }
        if (candidateMultiplier < 1 || minimumCandidates < 1) {
            throw new IllegalArgumentException("Reranking candidate properties must be positive");
        }
    }
}
