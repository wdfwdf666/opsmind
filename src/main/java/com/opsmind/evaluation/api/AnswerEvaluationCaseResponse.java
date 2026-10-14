package com.opsmind.evaluation.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.evaluation.domain.AnswerEvaluationCaseResult;
import com.opsmind.rag.application.RagRefusalReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AnswerEvaluationCaseResponse(
        String id /* 字段作用：对应对象的唯一标识。 */,
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        boolean answerable /* 字段作用：该不可变对象携带的 answerable 数据。 */,
        boolean refused /* 字段作用：该不可变对象携带的 refused 数据。 */,
        @Schema(nullable = true) RagRefusalReason refusalReason /* 字段作用：产生当前状态或决策的原因。 */,
        @Schema(nullable = true) Double topEvidenceScore /* 字段作用：相关性或质量分数，含义由当前算法定义。 */,
        @Schema(nullable = true) Double answerCorrectness /* 字段作用：该不可变对象携带的 answerCorrectness 数据。 */,
        @Schema(nullable = true, description = "Deterministic lexical support proxy, not a semantic faithfulness score")
        Double faithfulnessProxy /* 字段作用：该不可变对象携带的 faithfulnessProxy 数据。 */,
        List<String> expectedFacts /* 字段作用：该不可变对象携带的 expectedFacts 数据。 */,
        List<String> matchedFacts /* 字段作用：该不可变对象携带的 matchedFacts 数据。 */,
        List<String> expectedHeadingPaths /* 字段作用：该不可变对象携带的 expectedHeadingPaths 数据。 */,
        List<String> citedHeadingPaths /* 字段作用：该不可变对象携带的 citedHeadingPaths 数据。 */,
        double citationPrecision /* 字段作用：该不可变对象携带的 citationPrecision 数据。 */,
        double citationRecall /* 字段作用：该不可变对象携带的 citationRecall 数据。 */,
        double latencyMs /* 字段作用：该不可变对象携带的 latencyMs 数据。 */,
        int promptTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        int completionTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        int totalTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        String answer /* 字段作用：该不可变对象携带的 answer 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static AnswerEvaluationCaseResponse from(AnswerEvaluationCaseResult result) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new AnswerEvaluationCaseResponse(
                result.id(), result.query(), result.answerable(), result.refused(),
                result.refusalReason(), result.topEvidenceScore(),
                result.answerCorrectness(), result.faithfulnessProxy(), result.expectedFacts(), result.matchedFacts(),
                result.expectedHeadingPaths(), result.citedHeadingPaths(), result.citationPrecision(),
                result.citationRecall(), result.latencyMs(), result.promptTokens(), result.completionTokens(),
                result.totalTokens(), result.answer());
    }
}
