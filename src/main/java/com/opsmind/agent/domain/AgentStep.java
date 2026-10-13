package com.opsmind.agent.domain;

import java.time.Instant;
import java.util.UUID;

public record AgentStep(
        UUID id,
        UUID runId,
        int sequence,
        AgentStepType type,
        String toolName,
        AgentToolRisk toolRisk,
        boolean success,
        String detailJson,
        Instant createdAt) {
}
