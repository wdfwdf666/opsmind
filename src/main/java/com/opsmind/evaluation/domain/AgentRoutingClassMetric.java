package com.opsmind.evaluation.domain;

import com.opsmind.agent.domain.AgentTaskType;

public record AgentRoutingClassMetric(
        AgentTaskType taskType,
        int support,
        int predicted,
        int truePositive,
        double precision,
        double recall,
        double f1) {
}
