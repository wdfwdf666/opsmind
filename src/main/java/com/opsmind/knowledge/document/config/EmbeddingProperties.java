package com.opsmind.knowledge.document.config; // 包作用：配置层，把外部配置转换成 Spring 可管理的对象。

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量模型配置。
 * <p>record 参数名与 {@code opsmind.embedding.*} 配置键一一对应，例如 baseUrl 对应 base-url。</p>
 */
// Spring Boot 会执行“宽松绑定”：环境变量、短横线和驼峰命名可按规则互相转换。
@ConfigurationProperties(prefix = "opsmind.embedding") // 注解作用：把指定前缀的 YAML 或环境变量绑定到当前对象。
public record EmbeddingProperties(
        String provider /* 字段作用：当前选择的能力提供方式，例如 qwen 或 local。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        int dimension /* 字段作用：该不可变对象携带的 dimension 数据。 */,
        int defaultTopK /* 字段作用：该不可变对象携带的 defaultTopK 数据。 */,
        int maxTopK /* 字段作用：该不可变对象携带的 maxTopK 数据。 */,
        String baseUrl /* 字段作用：该不可变对象携带的 baseUrl 数据。 */,
        String apiKey /* 字段作用：该不可变对象携带的 apiKey 数据。 */,
        int batchSize /* 字段作用：该不可变对象携带的 batchSize 数据。 */) { // 类型作用：配置对象，字段值来自 application.yml 或 .env。

    // record 的紧凑构造器：绑定完成时立刻校验，配置错误会让应用启动失败而不是运行中才暴露。
    public EmbeddingProperties { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        if (provider == null || (!provider.equals("qwen") && !provider.equals("local"))) {
            throw new IllegalArgumentException("Embedding provider must be qwen or local");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Embedding model must not be blank");
        }
        if (dimension < 8) {
            throw new IllegalArgumentException("Embedding dimension must be at least 8");
        }
        if (defaultTopK < 1 || maxTopK < defaultTopK) {
            throw new IllegalArgumentException("Embedding top-k configuration is invalid");
        }
        if (batchSize < 1 || batchSize > 20) {
            throw new IllegalArgumentException("Embedding batch size must be between 1 and 20");
        }
    }
}
