package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;

public record RetrievalEvaluationCaseResult(
        String caseId /* 字段作用：对应对象的唯一标识。 */,
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        List<String> expectedHeadingPaths /* 字段作用：该不可变对象携带的 expectedHeadingPaths 数据。 */,
        List<String> retrievedHeadingPaths /* 字段作用：该不可变对象携带的 retrievedHeadingPaths 数据。 */,
        Integer firstRelevantRank /* 字段作用：该不可变对象携带的 firstRelevantRank 数据。 */,
        int candidateCount /* 字段作用：对应项目的数量统计。 */,
        double latencyMs /* 字段作用：该不可变对象携带的 latencyMs 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
