package com.opsmind.evaluation.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.evaluation.domain.AnswerEvaluationReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record AnswerEvaluationResponse(
        String dataset /* 字段作用：该不可变对象携带的 dataset 数据。 */,
        String description /* 字段作用：该不可变对象携带的 description 数据。 */,
        String retrievalModel /* 字段作用：该不可变对象携带的 retrievalModel 数据。 */,
        String generationModel /* 字段作用：该不可变对象携带的 generationModel 数据。 */,
        @Schema(format = "uuid", nullable = true) UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        int topK /* 字段作用：最终希望返回的最相关结果数量。 */,
        int caseCount /* 字段作用：对应项目的数量统计。 */,
        int answerableCaseCount /* 字段作用：对应项目的数量统计。 */,
        int unanswerableCaseCount /* 字段作用：对应项目的数量统计。 */,
        double answerCorrectness /* 字段作用：该不可变对象携带的 answerCorrectness 数据。 */,
        @Schema(description = "Deterministic character-bigram overlap with cited excerpts; not an LLM faithfulness judge")
        double faithfulnessProxy /* 字段作用：该不可变对象携带的 faithfulnessProxy 数据。 */,
        double citationPrecision /* 字段作用：该不可变对象携带的 citationPrecision 数据。 */,
        double citationRecall /* 字段作用：该不可变对象携带的 citationRecall 数据。 */,
        double refusalPrecision /* 字段作用：该不可变对象携带的 refusalPrecision 数据。 */,
        double refusalRecall /* 字段作用：该不可变对象携带的 refusalRecall 数据。 */,
        double refusalF1 /* 字段作用：该不可变对象携带的 refusalF1 数据。 */,
        double p50LatencyMs /* 字段作用：该不可变对象携带的 p50LatencyMs 数据。 */,
        double p95LatencyMs /* 字段作用：该不可变对象携带的 p95LatencyMs 数据。 */,
        int promptTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        int completionTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        int totalTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
        List<AnswerEvaluationCaseResponse> cases /* 字段作用：该不可变对象携带的 cases 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static AnswerEvaluationResponse from(AnswerEvaluationReport report) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new AnswerEvaluationResponse(
                report.dataset(), report.description(), report.retrievalModel(), report.generationModel(),
                report.documentId(), report.topK(), report.caseCount(), report.answerableCaseCount(),
                report.unanswerableCaseCount(), report.answerCorrectness(), report.faithfulnessProxy(),
                report.citationPrecision(), report.citationRecall(), report.refusalPrecision(), report.refusalRecall(),
                report.refusalF1(), report.p50LatencyMs(), report.p95LatencyMs(), report.promptTokens(),
                report.completionTokens(), report.totalTokens(),
                report.cases().stream().map(AnswerEvaluationCaseResponse::from).toList());
    }
}
