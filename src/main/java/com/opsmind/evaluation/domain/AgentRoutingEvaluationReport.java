package com.opsmind.evaluation.domain;

import java.util.List;

public record AgentRoutingEvaluationReport(
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
}
