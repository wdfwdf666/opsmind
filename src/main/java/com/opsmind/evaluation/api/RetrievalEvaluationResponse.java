package com.opsmind.evaluation.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.evaluation.domain.RetrievalEvaluationReport;
import com.opsmind.evaluation.domain.RetrievalMetricAtK;
import com.opsmind.evaluation.domain.RetrievalStrategy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record RetrievalEvaluationResponse(
        String dataset /* 字段作用：该不可变对象携带的 dataset 数据。 */,
        String description /* 字段作用：该不可变对象携带的 description 数据。 */,
        RetrievalStrategy strategy /* 字段作用：该不可变对象携带的 strategy 数据。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        @Schema(format = "uuid", nullable = true) UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        int caseCount /* 字段作用：对应项目的数量统计。 */,
        int maxK /* 字段作用：该不可变对象携带的 maxK 数据。 */,
        double averageCandidateCount /* 字段作用：对应项目的数量统计。 */,
        double p50LatencyMs /* 字段作用：该不可变对象携带的 p50LatencyMs 数据。 */,
        double p95LatencyMs /* 字段作用：该不可变对象携带的 p95LatencyMs 数据。 */,
        List<RetrievalMetricAtK> metrics /* 字段作用：该不可变对象携带的 metrics 数据。 */,
        List<RetrievalEvaluationCaseResponse> cases /* 字段作用：该不可变对象携带的 cases 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static RetrievalEvaluationResponse from(RetrievalEvaluationReport report) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new RetrievalEvaluationResponse(
                report.dataset(), report.description(), report.strategy(), report.model(), report.documentId(), report.caseCount(),
                report.maxK(), report.averageCandidateCount(), report.p50LatencyMs(), report.p95LatencyMs(),
                report.metrics(), report.cases().stream().map(RetrievalEvaluationCaseResponse::from).toList());
    }
}
