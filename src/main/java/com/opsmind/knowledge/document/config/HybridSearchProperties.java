package com.opsmind.knowledge.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.search.hybrid")
public record HybridSearchProperties(
        int rrfK,
        int candidateMultiplier,
        int minimumCandidates) {

    public HybridSearchProperties {
        if (rrfK < 1 || candidateMultiplier < 1 || minimumCandidates < 1) {
            throw new IllegalArgumentException("Hybrid search properties must be positive");
        }
    }
}
