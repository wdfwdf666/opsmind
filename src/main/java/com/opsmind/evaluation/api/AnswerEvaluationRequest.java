package com.opsmind.evaluation.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record AnswerEvaluationRequest(
        @Schema(example = "opsmind-answer-v1", nullable = true) String dataset /* 字段作用：该不可变对象携带的 dataset 数据。 */,
        @Schema(format = "uuid", nullable = true, description = "Optional document scope for reproducible evaluation")
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(example = "3", nullable = true, description = "Evidence count; dataset default is used when omitted")
        Integer topK /* 字段作用：最终希望返回的最相关结果数量。 */) { // 类型作用：请求 DTO，规定调用接口时可以提交哪些 JSON 字段。
}
