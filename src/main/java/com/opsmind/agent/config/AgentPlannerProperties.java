package com.opsmind.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.agent.planner")
public record AgentPlannerProperties(
        String provider,
        String model,
        String baseUrl,
        String apiKey,
        int maxOutputTokens,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxAttempts,
        boolean fallbackEnabled,
        String fallbackModel) {

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
        if (connectTimeoutMs < 100 || connectTimeoutMs > 30000
                || readTimeoutMs < 100 || readTimeoutMs > 120000) {
            throw new IllegalArgumentException("Agent planner timeouts are invalid");
        }
        if (maxAttempts < 1 || maxAttempts > 3) {
            throw new IllegalArgumentException("Agent planner max attempts must be between 1 and 3");
        }
        if (fallbackEnabled && (fallbackModel == null || fallbackModel.isBlank())) {
            throw new IllegalArgumentException("Agent planner fallback model must not be blank");
        }
    }

}
