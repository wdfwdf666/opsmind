package com.opsmind.evaluation.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import io.swagger.v3.oas.annotations.media.Schema;

public record AgentRoutingEvaluationRequest(
        @Schema(nullable = true, example = "opsmind-agent-routing-v1") String dataset /* 字段作用：该不可变对象携带的 dataset 数据。 */) { // 类型作用：请求 DTO，规定调用接口时可以提交哪些 JSON 字段。
}
