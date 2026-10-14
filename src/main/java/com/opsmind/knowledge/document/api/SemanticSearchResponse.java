package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record SemanticSearchResponse(
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        @Schema(format = "uuid", nullable = true) UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        int candidateCount /* 字段作用：对应项目的数量统计。 */,
        List<SemanticSearchHitResponse> hits /* 字段作用：该不可变对象携带的 hits 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    public static SemanticSearchResponse from(SemanticSearchResult result) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new SemanticSearchResponse(
                result.query(), result.model(), result.documentId(), result.candidateCount(),
                result.hits().stream().map(SemanticSearchHitResponse::from).toList());
    }
}
