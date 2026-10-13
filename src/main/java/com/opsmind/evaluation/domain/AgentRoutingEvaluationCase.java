package com.opsmind.evaluation.domain;

import com.opsmind.agent.domain.AgentTaskType;

public record AgentRoutingEvaluationCase(
        String id,
        String category,
        String query,
        AgentTaskType expectedTaskType) {
}
