package com.opsmind.agent.api;

import com.opsmind.agent.domain.AgentApproval;
import com.opsmind.agent.domain.AgentApprovalStatus;
import com.opsmind.agent.domain.AgentToolRisk;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record AgentApprovalResponse(
        UUID approvalId,
        String toolName,
        AgentToolRisk toolRisk,
        AgentApprovalStatus status,
        TicketDraftResponse draft,
        @Schema(nullable = true) String decisionReason,
        Instant createdAt,
        @Schema(nullable = true) Instant decidedAt) {

    static AgentApprovalResponse from(AgentApproval approval) {
        if (approval == null) {
            return null;
        }
        return new AgentApprovalResponse(
                approval.id(), approval.toolName(), approval.toolRisk(), approval.status(),
                TicketDraftResponse.from(approval.draft()), approval.decisionReason(),
                approval.createdAt(), approval.decidedAt());
    }
}
