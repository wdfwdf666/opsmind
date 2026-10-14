package com.opsmind.agent.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import io.swagger.v3.oas.annotations.media.Schema;

public record AgentApprovalRequest(
        @Schema(example = "APPROVE") AgentApprovalDecision decision /* 字段作用：该不可变对象携带的 decision 数据。 */,
        @Schema(nullable = true, example = "已确认影响范围，可以创建 P2 工单") String reason /* 字段作用：产生当前状态或决策的原因。 */) { // 类型作用：请求 DTO，规定调用接口时可以提交哪些 JSON 字段。
}
