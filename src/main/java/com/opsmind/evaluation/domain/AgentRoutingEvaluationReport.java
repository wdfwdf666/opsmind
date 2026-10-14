package com.opsmind.evaluation.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;

public record AgentRoutingEvaluationReport(
        String dataset /* 字段作用：该不可变对象携带的 dataset 数据。 */,
        String description /* 字段作用：该不可变对象携带的 description 数据。 */,
        String plannerProvider /* 字段作用：当前选择的能力提供方式，例如 qwen 或 local。 */,
        String plannerModel /* 字段作用：该不可变对象携带的 plannerModel 数据。 */,
        int caseCount /* 字段作用：对应项目的数量统计。 */,
        int correctCount /* 字段作用：对应项目的数量统计。 */,
        double accuracy /* 字段作用：该不可变对象携带的 accuracy 数据。 */,
        double macroPrecision /* 字段作用：该不可变对象携带的 macroPrecision 数据。 */,
        double macroRecall /* 字段作用：该不可变对象携带的 macroRecall 数据。 */,
        double macroF1 /* 字段作用：该不可变对象携带的 macroF1 数据。 */,
        int dangerousWriteMisrouteCount /* 字段作用：对应项目的数量统计。 */,
        double dangerousWriteMisrouteRate /* 字段作用：该不可变对象携带的 dangerousWriteMisrouteRate 数据。 */,
        double p50LatencyMs /* 字段作用：该不可变对象携带的 p50LatencyMs 数据。 */,
        double p95LatencyMs /* 字段作用：该不可变对象携带的 p95LatencyMs 数据。 */,
        List<AgentRoutingClassMetric> classMetrics /* 字段作用：该不可变对象携带的 classMetrics 数据。 */,
        List<AgentRoutingEvaluationCaseResult> cases /* 字段作用：该不可变对象携带的 cases 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
