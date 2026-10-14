package com.opsmind.rag.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.rag.application.RagAnswerResult;
import com.opsmind.rag.application.RagRefusalReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record RagAnswerResponse(
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        @Schema(format = "uuid", nullable = true) UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        String retrievalModel /* 字段作用：该不可变对象携带的 retrievalModel 数据。 */,
        String generationModel /* 字段作用：该不可变对象携带的 generationModel 数据。 */,
        boolean refused /* 字段作用：该不可变对象携带的 refused 数据。 */,
        @Schema(nullable = true) RagRefusalReason refusalReason /* 字段作用：产生当前状态或决策的原因。 */,
        @Schema(nullable = true, description = "Top reranker score used by the evidence sufficiency gate")
        Double topEvidenceScore /* 字段作用：相关性或质量分数，含义由当前算法定义。 */,
        String answer /* 字段作用：该不可变对象携带的 answer 数据。 */,
        List<RagCitationResponse> citations /* 字段作用：该不可变对象携带的 citations 数据。 */,
        Integer promptTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        Integer completionTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        Integer totalTokens /* 字段作用：模型输入或输出的 Token 数量。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static RagAnswerResponse from(RagAnswerResult result) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new RagAnswerResponse(
                result.query(), result.documentId(), result.retrievalModel(), result.generationModel(),
                result.refused(), result.refusalReason(), result.topEvidenceScore(), result.answer(),
                result.citations().stream().map(RagCitationResponse::from).toList(),
                result.promptTokens(), result.completionTokens(), result.totalTokens());
    }
}
