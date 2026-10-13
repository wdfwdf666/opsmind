package com.opsmind.evaluation.application;

import com.opsmind.agent.application.AgentPlan;
import com.opsmind.agent.application.AgentPlanner;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.evaluation.domain.AgentRoutingClassMetric;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationCaseResult;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationDataset;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;

@Service
public class AgentRoutingEvaluationService {

    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private final AgentRoutingEvaluationDatasetLoader datasetLoader;
    private final AgentPlanner planner;
    private final LongSupplier nanoTime;

    @Autowired
    public AgentRoutingEvaluationService(
            AgentRoutingEvaluationDatasetLoader datasetLoader, AgentPlanner planner) {
        this(datasetLoader, planner, System::nanoTime);
    }

    AgentRoutingEvaluationService(
            AgentRoutingEvaluationDatasetLoader datasetLoader,
            AgentPlanner planner,
            LongSupplier nanoTime) {
        this.datasetLoader = datasetLoader;
        this.planner = planner;
        this.nanoTime = nanoTime;
    }

    public AgentRoutingEvaluationReport run(String datasetName) {
        AgentRoutingEvaluationDataset dataset = datasetLoader.load(datasetName);
        List<AgentRoutingEvaluationCaseResult> results = new ArrayList<>(dataset.cases().size());
        String provider = null;
        String model = null;
        for (var evaluationCase : dataset.cases()) {
            long startedAt = nanoTime.getAsLong();
            AgentPlan plan = planner.plan(evaluationCase.query());
            double latencyMs = (nanoTime.getAsLong() - startedAt) / NANOS_PER_MILLISECOND;
            provider = provider == null ? plan.provider() : provider;
            model = model == null ? plan.model() : model;
            boolean dangerousMisroute = evaluationCase.expectedTaskType() == AgentTaskType.KNOWLEDGE_QA
                    && plan.taskType() == AgentTaskType.CREATE_TICKET;
            results.add(new AgentRoutingEvaluationCaseResult(
                    evaluationCase.id(), evaluationCase.category(), evaluationCase.query(),
                    evaluationCase.expectedTaskType(), plan.taskType(),
                    evaluationCase.expectedTaskType() == plan.taskType(), dangerousMisroute,
                    plan.reason(), latencyMs));
        }
        return report(dataset, provider, model, results);
    }

    private AgentRoutingEvaluationReport report(
            AgentRoutingEvaluationDataset dataset,
            String provider,
            String model,
            List<AgentRoutingEvaluationCaseResult> results) {
        List<AgentRoutingClassMetric> classMetrics = Arrays.stream(AgentTaskType.values())
                .map(taskType -> classMetric(taskType, results))
                .toList();
        int correct = (int) results.stream().filter(AgentRoutingEvaluationCaseResult::correct).count();
        int dangerous = (int) results.stream()
                .filter(AgentRoutingEvaluationCaseResult::dangerousWriteMisroute).count();
        long knowledgeCases = results.stream()
                .filter(result -> result.expectedTaskType() == AgentTaskType.KNOWLEDGE_QA).count();
        List<Double> latencies = results.stream()
                .map(AgentRoutingEvaluationCaseResult::latencyMs).sorted().toList();
        return new AgentRoutingEvaluationReport(
                dataset.name(), dataset.description(), provider, model, results.size(), correct,
                divide(correct, results.size()),
                classMetrics.stream().mapToDouble(AgentRoutingClassMetric::precision).average().orElse(0),
                classMetrics.stream().mapToDouble(AgentRoutingClassMetric::recall).average().orElse(0),
                classMetrics.stream().mapToDouble(AgentRoutingClassMetric::f1).average().orElse(0),
                dangerous, divide(dangerous, knowledgeCases),
                percentile(latencies, 0.50), percentile(latencies, 0.95),
                classMetrics, List.copyOf(results));
    }

    private AgentRoutingClassMetric classMetric(
            AgentTaskType taskType, List<AgentRoutingEvaluationCaseResult> results) {
        int support = (int) results.stream().filter(result -> result.expectedTaskType() == taskType).count();
        int predicted = (int) results.stream().filter(result -> result.actualTaskType() == taskType).count();
        int truePositive = (int) results.stream().filter(result -> result.expectedTaskType() == taskType
                && result.actualTaskType() == taskType).count();
        double precision = divide(truePositive, predicted);
        double recall = divide(truePositive, support);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        return new AgentRoutingClassMetric(taskType, support, predicted, truePositive, precision, recall, f1);
    }

    private double divide(long numerator, long denominator) {
        return denominator == 0 ? 0 : numerator / (double) denominator;
    }

    private double percentile(List<Double> sortedValues, double percentile) {
        int index = Math.max(0, (int) Math.ceil(percentile * sortedValues.size()) - 1);
        return sortedValues.get(index);
    }
}
