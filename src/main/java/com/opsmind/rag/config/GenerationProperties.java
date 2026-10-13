package com.opsmind.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.generation")
public record GenerationProperties(
        String provider,
        String model,
        String baseUrl,
        String apiKey,
        int evidenceTopK,
        double minimumEvidenceScore,
        int maxContextCharacters,
        int maxOutputTokens,
        double temperature) {

    public GenerationProperties {
        if (provider == null || (!provider.equals("qwen") && !provider.equals("local"))) {
            throw new IllegalArgumentException("Generation provider must be qwen or local");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Generation model must not be blank");
        }
        if (evidenceTopK < 1 || evidenceTopK > 50) {
            throw new IllegalArgumentException("Generation evidence top-k must be between 1 and 50");
        }
        if (!Double.isFinite(minimumEvidenceScore) || minimumEvidenceScore < 0 || minimumEvidenceScore > 1) {
            throw new IllegalArgumentException("Generation minimum evidence score must be between 0 and 1");
        }
        if (maxContextCharacters < 1000 || maxOutputTokens < 1) {
            throw new IllegalArgumentException("Generation context and output budgets are invalid");
        }
        if (temperature < 0 || temperature > 2) {
            throw new IllegalArgumentException("Generation temperature must be between 0 and 2");
        }
    }
}
