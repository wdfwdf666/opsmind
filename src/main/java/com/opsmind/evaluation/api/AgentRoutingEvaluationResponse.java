package com.opsmind.evaluation.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.evaluation.domain.AgentRoutingClassMetric;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationCaseResult;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationReport;

import java.util.List;

public record AgentRoutingEvaluationResponse(
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
        List<AgentRoutingEvaluationCaseResult> cases /* 字段作用：该不可变对象携带的 cases 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static AgentRoutingEvaluationResponse from(AgentRoutingEvaluationReport report) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new AgentRoutingEvaluationResponse(
                report.dataset(), report.description(), report.plannerProvider(), report.plannerModel(),
                report.caseCount(), report.correctCount(), report.accuracy(), report.macroPrecision(),
                report.macroRecall(), report.macroF1(), report.dangerousWriteMisrouteCount(),
                report.dangerousWriteMisrouteRate(), report.p50LatencyMs(), report.p95LatencyMs(),
                report.classMetrics(), report.cases());
    }
}
