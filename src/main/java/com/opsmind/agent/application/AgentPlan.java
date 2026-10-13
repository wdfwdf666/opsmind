package com.opsmind.agent.application;

import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentTaskTypeSource;

public record AgentPlan(
        AgentTaskType taskType,
        AgentTaskTypeSource source,
        String provider,
        String model,
        String reason) {

    public static AgentPlan explicit(AgentTaskType taskType) {
        return new AgentPlan(taskType, AgentTaskTypeSource.EXPLICIT, "request", "explicit-override",
                "调用方显式指定任务类型");
    }
}
