package com.opsmind.agent.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.agent.domain.AgentApproval;
import com.opsmind.agent.domain.AgentApprovalStatus;
import com.opsmind.agent.domain.AgentToolRisk;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record AgentApprovalResponse(
        UUID approvalId /* 字段作用：对应对象的唯一标识。 */,
        String toolName /* 字段作用：该不可变对象携带的 toolName 数据。 */,
        AgentToolRisk toolRisk /* 字段作用：该不可变对象携带的 toolRisk 数据。 */,
        AgentApprovalStatus status /* 字段作用：当前业务状态。 */,
        TicketDraftResponse draft /* 字段作用：该不可变对象携带的 draft 数据。 */,
        @Schema(nullable = true) String decisionReason /* 字段作用：产生当前状态或决策的原因。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */,
        @Schema(nullable = true) Instant decidedAt /* 字段作用：最近更新或完成时间。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static AgentApprovalResponse from(AgentApproval approval) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        if (approval == null) {
            return null;
        }
        return new AgentApprovalResponse(
                approval.id(), approval.toolName(), approval.toolRisk(), approval.status(),
                TicketDraftResponse.from(approval.draft()), approval.decisionReason(),
                approval.createdAt(), approval.decidedAt());
    }
}
