package com.opsmind.evaluation.api;

import com.opsmind.evaluation.domain.AgentRoutingClassMetric;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationCaseResult;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationReport;

import java.util.List;

public record AgentRoutingEvaluationResponse(
        String dataset,
        String description,
        String plannerProvider,
        String plannerModel,
        int caseCount,
        int correctCount,
        double accuracy,
        double macroPrecision,
        double macroRecall,
        double macroF1,
        int dangerousWriteMisrouteCount,
        double dangerousWriteMisrouteRate,
        double p50LatencyMs,
        double p95LatencyMs,
        List<AgentRoutingClassMetric> classMetrics,
        List<AgentRoutingEvaluationCaseResult> cases) {

    static AgentRoutingEvaluationResponse from(AgentRoutingEvaluationReport report) {
        return new AgentRoutingEvaluationResponse(
                report.dataset(), report.description(), report.plannerProvider(), report.plannerModel(),
                report.caseCount(), report.correctCount(), report.accuracy(), report.macroPrecision(),
                report.macroRecall(), report.macroF1(), report.dangerousWriteMisrouteCount(),
                report.dangerousWriteMisrouteRate(), report.p50LatencyMs(), report.p95LatencyMs(),
                report.classMetrics(), report.cases());
    }
}
