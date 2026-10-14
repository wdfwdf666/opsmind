package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;
import java.util.UUID;

public record RetrievalEvaluationReport(
        String dataset /* 字段作用：该不可变对象携带的 dataset 数据。 */,
        String description /* 字段作用：该不可变对象携带的 description 数据。 */,
        RetrievalStrategy strategy /* 字段作用：该不可变对象携带的 strategy 数据。 */,
        String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        int caseCount /* 字段作用：对应项目的数量统计。 */,
        int maxK /* 字段作用：该不可变对象携带的 maxK 数据。 */,
        double averageCandidateCount /* 字段作用：对应项目的数量统计。 */,
        double p50LatencyMs /* 字段作用：该不可变对象携带的 p50LatencyMs 数据。 */,
        double p95LatencyMs /* 字段作用：该不可变对象携带的 p95LatencyMs 数据。 */,
        List<RetrievalMetricAtK> metrics /* 字段作用：该不可变对象携带的 metrics 数据。 */,
        List<RetrievalEvaluationCaseResult> cases /* 字段作用：该不可变对象携带的 cases 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
