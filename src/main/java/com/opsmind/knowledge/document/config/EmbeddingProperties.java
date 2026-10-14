package com.opsmind.knowledge.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量模型配置。
 * <p>record 参数名与 {@code opsmind.embedding.*} 配置键一一对应，例如 baseUrl 对应 base-url。</p>
 */
// Spring Boot 会执行“宽松绑定”：环境变量、短横线和驼峰命名可按规则互相转换。
@ConfigurationProperties(prefix = "opsmind.embedding")
public record EmbeddingProperties(
        String provider,
        String model,
        int dimension,
        int defaultTopK,
        int maxTopK,
        String baseUrl,
        String apiKey,
        int batchSize) {

    // record 的紧凑构造器：绑定完成时立刻校验，配置错误会让应用启动失败而不是运行中才暴露。
    public EmbeddingProperties {
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
