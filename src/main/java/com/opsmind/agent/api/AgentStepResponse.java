package com.opsmind.agent.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.agent.domain.AgentStep;
import com.opsmind.agent.domain.AgentStepType;
import com.opsmind.agent.domain.AgentToolRisk;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record AgentStepResponse(
        UUID id /* 字段作用：对应对象的唯一标识。 */,
        int sequence /* 字段作用：该不可变对象携带的 sequence 数据。 */,
        AgentStepType type /* 字段作用：该不可变对象携带的 type 数据。 */,
        @Schema(nullable = true) String toolName /* 字段作用：该不可变对象携带的 toolName 数据。 */,
        @Schema(nullable = true) AgentToolRisk toolRisk /* 字段作用：该不可变对象携带的 toolRisk 数据。 */,
        boolean success /* 字段作用：该不可变对象携带的 success 数据。 */,
        @Schema(description = "Compact JSON audit metadata; prompts and evidence bodies are not stored") String detailJson /* 字段作用：该不可变对象携带的 detailJson 数据。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static AgentStepResponse from(AgentStep step) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new AgentStepResponse(
                step.id(), step.sequence(), step.type(), step.toolName(), step.toolRisk(),
                step.success(), step.detailJson(), step.createdAt());
    }
}
