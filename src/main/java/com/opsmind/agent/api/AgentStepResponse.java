package com.opsmind.agent.api;

import com.opsmind.agent.domain.AgentStep;
import com.opsmind.agent.domain.AgentStepType;
import com.opsmind.agent.domain.AgentToolRisk;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record AgentStepResponse(
        UUID id,
        int sequence,
        AgentStepType type,
        @Schema(nullable = true) String toolName,
        @Schema(nullable = true) AgentToolRisk toolRisk,
        boolean success,
        @Schema(description = "Compact JSON audit metadata; prompts and evidence bodies are not stored") String detailJson,
        Instant createdAt) {

    static AgentStepResponse from(AgentStep step) {
        return new AgentStepResponse(
                step.id(), step.sequence(), step.type(), step.toolName(), step.toolRisk(),
                step.success(), step.detailJson(), step.createdAt());
    }
}
