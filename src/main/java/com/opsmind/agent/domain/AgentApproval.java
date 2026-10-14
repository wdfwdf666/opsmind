package com.opsmind.agent.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.time.Instant;
import java.util.UUID;

public record AgentApproval(
        UUID id /* 字段作用：对应对象的唯一标识。 */,
        UUID runId /* 字段作用：对应对象的唯一标识。 */,
        String toolName /* 字段作用：该不可变对象携带的 toolName 数据。 */,
        AgentToolRisk toolRisk /* 字段作用：该不可变对象携带的 toolRisk 数据。 */,
        AgentApprovalStatus status /* 字段作用：当前业务状态。 */,
        TicketDraft draft /* 字段作用：该不可变对象携带的 draft 数据。 */,
        String decisionReason /* 字段作用：产生当前状态或决策的原因。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */,
        Instant decidedAt /* 字段作用：最近更新或完成时间。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
