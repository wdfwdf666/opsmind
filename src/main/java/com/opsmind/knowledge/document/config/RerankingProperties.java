package com.opsmind.knowledge.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.reranking")
public record RerankingProperties(
        String provider,
        String model,
        String endpoint,
        String apiKey,
        String instruction,
        int candidateMultiplier,
        int minimumCandidates) {

    public RerankingProperties {
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
