package com.opsmind.agent.api;

import com.opsmind.agent.domain.AgentRun;
import com.opsmind.agent.domain.AgentRunStatus;
import com.opsmind.agent.domain.AgentRunView;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.rag.application.RagRefusalReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentRunResponse(
        UUID runId,
        String query,
        @Schema(format = "uuid", nullable = true) UUID documentId,
        AgentTaskType taskType,
        AgentRunStatus status,
        @Schema(nullable = true) String answer,
        @Schema(nullable = true) Boolean refused,
        @Schema(nullable = true) RagRefusalReason refusalReason,
        @Schema(nullable = true) Double topEvidenceScore,
        List<AgentCitationResponse> citations,
        @Schema(nullable = true) String errorCode,
        @Schema(nullable = true) String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        @Schema(nullable = true) Instant completedAt,
        List<AgentStepResponse> steps,
        @Schema(nullable = true) AgentApprovalResponse approval,
        @Schema(nullable = true) SupportTicketResponse ticket) {

    static AgentRunResponse from(AgentRunView view) {
        AgentRun run = view.run();
        return new AgentRunResponse(
                run.id(), run.query(), run.documentId(), run.taskType(), run.status(), run.answer(), run.refused(),
                run.refusalReason(), run.topEvidenceScore(),
                run.citations().stream().map(AgentCitationResponse::from).toList(),
                run.errorCode(), run.errorMessage(), run.createdAt(), run.updatedAt(), run.completedAt(),
                view.steps().stream().map(AgentStepResponse::from).toList(),
                AgentApprovalResponse.from(view.approval()), SupportTicketResponse.from(view.ticket()));
    }
}
