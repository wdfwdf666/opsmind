package com.opsmind.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record AgentRoutingEvaluationRequest(
        @Schema(nullable = true, example = "opsmind-agent-routing-v1") String dataset) {
}
