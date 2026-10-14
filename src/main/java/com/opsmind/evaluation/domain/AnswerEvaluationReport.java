package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;
import java.util.UUID;

public record AnswerEvaluationReport(
        String dataset /* 字段作用：该不可变对象携带的 dataset 数据。 */,
        String description /* 字段作用：该不可变对象携带的 description 数据。 */,
        String retrievalModel /* 字段作用：该不可变对象携带的 retrievalModel 数据。 */,
        String generationModel /* 字段作用：该不可变对象携带的 generationModel 数据。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        int topK /* 字段作用：最终希望返回的最相关结果数量。 */,
        int caseCount /* 字段作用：对应项目的数量统计。 */,
        int answerableCaseCount /* 字段作用：对应项目的数量统计。 */,
        int unanswerableCaseCount /* 字段作用：对应项目的数量统计。 */,
        double answerCorrectness /* 字段作用：该不可变对象携带的 answerCorrectness 数据。 */,
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
        List<AnswerEvaluationCaseResult> cases /* 字段作用：该不可变对象携带的 cases 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
