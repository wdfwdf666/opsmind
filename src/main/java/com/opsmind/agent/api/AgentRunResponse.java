package com.opsmind.agent.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.agent.domain.AgentRun;
import com.opsmind.agent.domain.AgentRunStatus;
import com.opsmind.agent.domain.AgentRunView;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.AgentTaskTypeSource;
import com.opsmind.rag.application.RagRefusalReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentRunResponse(
        UUID runId /* 字段作用：对应对象的唯一标识。 */,
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        @Schema(format = "uuid", nullable = true) UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        AgentTaskType taskType /* 字段作用：该不可变对象携带的 taskType 数据。 */,
        AgentTaskTypeSource taskTypeSource /* 字段作用：该不可变对象携带的 taskTypeSource 数据。 */,
        String plannerProvider /* 字段作用：当前选择的能力提供方式，例如 qwen 或 local。 */,
        String plannerModel /* 字段作用：该不可变对象携带的 plannerModel 数据。 */,
        String plannerReason /* 字段作用：产生当前状态或决策的原因。 */,
        AgentRunStatus status /* 字段作用：当前业务状态。 */,
        @Schema(nullable = true) String answer /* 字段作用：该不可变对象携带的 answer 数据。 */,
        @Schema(nullable = true) Boolean refused /* 字段作用：该不可变对象携带的 refused 数据。 */,
        @Schema(nullable = true) RagRefusalReason refusalReason /* 字段作用：产生当前状态或决策的原因。 */,
        @Schema(nullable = true) Double topEvidenceScore /* 字段作用：相关性或质量分数，含义由当前算法定义。 */,
        List<AgentCitationResponse> citations /* 字段作用：该不可变对象携带的 citations 数据。 */,
        @Schema(nullable = true) String errorCode /* 字段作用：该不可变对象携带的 errorCode 数据。 */,
        @Schema(nullable = true) String errorMessage /* 字段作用：该不可变对象携带的 errorMessage 数据。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */,
        Instant updatedAt /* 字段作用：最近更新或完成时间。 */,
        @Schema(nullable = true) Instant completedAt /* 字段作用：最近更新或完成时间。 */,
        List<AgentStepResponse> steps /* 字段作用：该不可变对象携带的 steps 数据。 */,
        @Schema(nullable = true) AgentApprovalResponse approval /* 字段作用：该不可变对象携带的 approval 数据。 */,
        @Schema(nullable = true) SupportTicketResponse ticket /* 字段作用：该不可变对象携带的 ticket 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static AgentRunResponse from(AgentRunView view) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        AgentRun run = view.run();
        return new AgentRunResponse(
                run.id(), run.query(), run.documentId(), run.taskType(), run.taskTypeSource(), run.plannerProvider(),
                run.plannerModel(), run.plannerReason(), run.status(), run.answer(), run.refused(),
                run.refusalReason(), run.topEvidenceScore(),
                run.citations().stream().map(AgentCitationResponse::from).toList(),
                run.errorCode(), run.errorMessage(), run.createdAt(), run.updatedAt(), run.completedAt(),
                view.steps().stream().map(AgentStepResponse::from).toList(),
                AgentApprovalResponse.from(view.approval()), SupportTicketResponse.from(view.ticket()));
    }
}
