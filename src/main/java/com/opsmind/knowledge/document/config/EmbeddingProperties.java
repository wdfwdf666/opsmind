package com.opsmind.knowledge.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
