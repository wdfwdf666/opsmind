package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import com.opsmind.rag.application.RagRefusalReason;

import java.util.List;

public record AnswerEvaluationCaseResult(
        String id /* 字段作用：对应对象的唯一标识。 */,
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        boolean answerable /* 字段作用：该不可变对象携带的 answerable 数据。 */,
        boolean refused /* 字段作用：该不可变对象携带的 refused 数据。 */,
        RagRefusalReason refusalReason /* 字段作用：产生当前状态或决策的原因。 */,
        Double topEvidenceScore /* 字段作用：相关性或质量分数，含义由当前算法定义。 */,
        Double answerCorrectness /* 字段作用：该不可变对象携带的 answerCorrectness 数据。 */,
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
        String answer /* 字段作用：该不可变对象携带的 answer 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
