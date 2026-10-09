package com.opsmind.knowledge.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.embedding")
public record EmbeddingProperties(
        String model,
        int dimension,
        int defaultTopK,
        int maxTopK) {

    public EmbeddingProperties {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Embedding model must not be blank");
        }
        if (dimension < 8) {
            throw new IllegalArgumentException("Embedding dimension must be at least 8");
        }
        if (defaultTopK < 1 || maxTopK < defaultTopK) {
            throw new IllegalArgumentException("Embedding top-k configuration is invalid");
        }
    }
}
