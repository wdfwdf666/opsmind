package com.opsmind.agent.domain;

import java.util.List;

public record AgentRunView(
        AgentRun run,
        List<AgentStep> steps,
        AgentApproval approval,
        SupportTicket ticket) {
}
