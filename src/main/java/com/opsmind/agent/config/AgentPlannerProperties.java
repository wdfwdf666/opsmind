package com.opsmind.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.agent.planner")
public record AgentPlannerProperties(
        String provider,
        String model,
        String baseUrl,
        String apiKey,
        int maxOutputTokens) {

    public AgentPlannerProperties {
        if (provider == null || (!provider.equals("qwen") && !provider.equals("local"))) {
            throw new IllegalArgumentException("Agent planner provider must be qwen or local");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Agent planner model must not be blank");
        }
        if (maxOutputTokens < 32 || maxOutputTokens > 1000) {
            throw new IllegalArgumentException("Agent planner max output tokens must be between 32 and 1000");
        }
    }
}
