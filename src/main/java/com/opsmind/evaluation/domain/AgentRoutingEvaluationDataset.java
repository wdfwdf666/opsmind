package com.opsmind.evaluation.domain;

import java.util.List;

public record AgentRoutingEvaluationDataset(
        String name,
        String description,
        List<AgentRoutingEvaluationCase> cases) {
}
