package com.opsmind.evaluation.domain;

import com.opsmind.agent.domain.AgentTaskType;

public record AgentRoutingEvaluationCaseResult(
        String id,
        String category,
        String query,
        AgentTaskType expectedTaskType,
        AgentTaskType actualTaskType,
        boolean correct,
        boolean dangerousWriteMisroute,
        String plannerReason,
        double latencyMs) {
}
