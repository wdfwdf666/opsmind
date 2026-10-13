package com.opsmind.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record AgentApprovalRequest(
        @Schema(example = "APPROVE") AgentApprovalDecision decision,
        @Schema(nullable = true, example = "已确认影响范围，可以创建 P2 工单") String reason) {
}
