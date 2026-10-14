package com.opsmind.evaluation.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.evaluation.domain.RetrievalEvaluationCaseResult;

import java.util.List;

public record RetrievalEvaluationCaseResponse(
        String caseId /* 字段作用：对应对象的唯一标识。 */,
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        List<String> expectedHeadingPaths /* 字段作用：该不可变对象携带的 expectedHeadingPaths 数据。 */,
        List<String> retrievedHeadingPaths /* 字段作用：该不可变对象携带的 retrievedHeadingPaths 数据。 */,
        Integer firstRelevantRank /* 字段作用：该不可变对象携带的 firstRelevantRank 数据。 */,
        int candidateCount /* 字段作用：对应项目的数量统计。 */,
        double latencyMs /* 字段作用：该不可变对象携带的 latencyMs 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static RetrievalEvaluationCaseResponse from(RetrievalEvaluationCaseResult result) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new RetrievalEvaluationCaseResponse(
                result.caseId(), result.query(), result.expectedHeadingPaths(), result.retrievedHeadingPaths(),
                result.firstRelevantRank(), result.candidateCount(), result.latencyMs());
    }
}
