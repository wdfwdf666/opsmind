package com.opsmind.agent.domain;

import java.time.Instant;
import java.util.UUID;

public record AgentApproval(
        UUID id,
        UUID runId,
        String toolName,
        AgentToolRisk toolRisk,
        AgentApprovalStatus status,
        TicketDraft draft,
        String decisionReason,
        Instant createdAt,
        Instant decidedAt) {
}
