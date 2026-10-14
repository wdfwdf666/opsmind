package com.opsmind.evaluation.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.evaluation.domain.AnswerEvaluationCase;
import com.opsmind.evaluation.domain.AnswerEvaluationCaseResult;
import com.opsmind.evaluation.domain.AnswerEvaluationDataset;
import com.opsmind.evaluation.domain.AnswerEvaluationReport;
import com.opsmind.evaluation.domain.EvaluationException;
import com.opsmind.rag.application.RagAnswerResult;
import com.opsmind.rag.application.RagAnswerService;
import com.opsmind.rag.application.RagCitation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

@Service // 注解作用：把类注册为业务服务，由 Spring 负责创建和注入。
public class AnswerEvaluationService { // 类型作用：应用服务，负责组织一个完整业务流程。

    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private static final Pattern CITATION = Pattern.compile("\\[S\\d+]", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_CONTENT = Pattern.compile("[^\\p{L}\\p{N}]");

    private final AnswerEvaluationDatasetLoader datasetLoader;
    private final RagAnswerService answerService;
    private final LongSupplier nanoTime;

    @Autowired // 注解作用：要求 Spring 按构造器参数类型注入依赖。
    public AnswerEvaluationService(AnswerEvaluationDatasetLoader datasetLoader, RagAnswerService answerService) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this(datasetLoader, answerService, System::nanoTime);
    }

    AnswerEvaluationService(
            AnswerEvaluationDatasetLoader datasetLoader,
            RagAnswerService answerService,
            LongSupplier nanoTime) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.datasetLoader = datasetLoader;
        this.answerService = answerService;
        this.nanoTime = nanoTime;
    }

    public AnswerEvaluationReport run(String datasetName, UUID documentId, Integer requestedTopK) { // 方法作用：加载固定评测集，逐条执行并汇总质量与延迟指标。
        AnswerEvaluationDataset dataset = datasetLoader.load(datasetName);
        int topK = requestedTopK == null ? dataset.topK() : requestedTopK;
        if (topK < 1 || topK > 50) {
            throw new EvaluationException(
                    "EVALUATION_TOP_K_INVALID", "Evaluation topK must be between 1 and 50", HttpStatus.BAD_REQUEST);
        }

        List<AnswerEvaluationCaseResult> results = new ArrayList<>(dataset.cases().size());
        String retrievalModel = null;
        String generationModel = null;
        for (AnswerEvaluationCase evaluationCase : dataset.cases()) {
            long startedAt = nanoTime.getAsLong();
            RagAnswerResult answer = answerService.answer(evaluationCase.query(), topK, documentId);
            double latencyMs = (nanoTime.getAsLong() - startedAt) / NANOS_PER_MILLISECOND;
            retrievalModel = retrievalModel == null ? answer.retrievalModel() : retrievalModel;
            generationModel = generationModel == null ? answer.generationModel() : generationModel;
            results.add(toCaseResult(evaluationCase, answer, latencyMs));
        }
        return report(dataset, documentId, topK, retrievalModel, generationModel, results);
    }

    private AnswerEvaluationCaseResult toCaseResult(
            AnswerEvaluationCase evaluationCase, RagAnswerResult answer, double latencyMs) { // 方法作用：把外部或数据库数据转换成项目内部对象。
        List<String> expectedFacts = evaluationCase.expectedFactGroups().stream().map(group -> group.getFirst()).toList();
        List<String> matchedFacts = evaluationCase.expectedFactGroups().stream()
                .filter(group -> matchesAny(answer.answer(), group))
                .map(group -> group.getFirst())
                .toList();
        Double correctness = evaluationCase.answerable()
                ? matchedFacts.size() / (double) evaluationCase.expectedFactGroups().size()
                : null;
        Double faithfulness = evaluationCase.answerable() && !answer.refused()
                ? faithfulnessProxy(answer.answer(), answer.citations())
                : null;

        List<String> citedHeadings = answer.citations().stream().map(RagCitation::headingPath).distinct().toList();
        Set<String> expectedHeadings = new HashSet<>(evaluationCase.expectedHeadingPaths());
        long relevantCitations = citedHeadings.stream().filter(expectedHeadings::contains).count();
        double citationPrecision = citedHeadings.isEmpty() ? 0 : relevantCitations / (double) citedHeadings.size();
        double citationRecall = expectedHeadings.isEmpty() ? 0 : relevantCitations / (double) expectedHeadings.size();

        return new AnswerEvaluationCaseResult(
                evaluationCase.id(), evaluationCase.query(), evaluationCase.answerable(), answer.refused(),
                answer.refusalReason(), answer.topEvidenceScore(),
                correctness, faithfulness, expectedFacts, matchedFacts,
                List.copyOf(evaluationCase.expectedHeadingPaths()), citedHeadings,
                citationPrecision, citationRecall, latencyMs,
                value(answer.promptTokens()), value(answer.completionTokens()), value(answer.totalTokens()), answer.answer());
    }

    private AnswerEvaluationReport report(
            AnswerEvaluationDataset dataset,
            UUID documentId,
            int topK,
            String retrievalModel,
            String generationModel,
            List<AnswerEvaluationCaseResult> results) { // 方法作用：完成“report”对应的内部步骤，供主流程复用。
        List<AnswerEvaluationCaseResult> answerable = results.stream().filter(AnswerEvaluationCaseResult::answerable).toList();
        List<AnswerEvaluationCaseResult> unanswerable = results.stream().filter(result -> !result.answerable()).toList();
        long correctCitations = results.stream().mapToLong(this::correctCitationCount).sum();
        long actualCitations = results.stream().mapToLong(result -> result.citedHeadingPaths().size()).sum();
        long expectedCitations = results.stream().mapToLong(result -> result.expectedHeadingPaths().size()).sum();
        long trueRefusals = unanswerable.stream().filter(AnswerEvaluationCaseResult::refused).count();
        long allRefusals = results.stream().filter(AnswerEvaluationCaseResult::refused).count();
        double refusalPrecision = divide(trueRefusals, allRefusals);
        double refusalRecall = divide(trueRefusals, unanswerable.size());
        double refusalF1 = refusalPrecision + refusalRecall == 0
                ? 0
                : 2 * refusalPrecision * refusalRecall / (refusalPrecision + refusalRecall);
        List<Double> latencies = results.stream().map(AnswerEvaluationCaseResult::latencyMs).sorted().toList();

        return new AnswerEvaluationReport(
                dataset.name(), dataset.description(), retrievalModel, generationModel, documentId, topK,
                results.size(), answerable.size(), unanswerable.size(),
                answerable.stream().map(AnswerEvaluationCaseResult::answerCorrectness)
                        .mapToDouble(Double::doubleValue).average().orElse(0),
                answerable.stream().map(AnswerEvaluationCaseResult::faithfulnessProxy)
                        .filter(value -> value != null).mapToDouble(Double::doubleValue).average().orElse(0),
                divide(correctCitations, actualCitations), divide(correctCitations, expectedCitations),
                refusalPrecision, refusalRecall, refusalF1,
                percentile(latencies, 0.50), percentile(latencies, 0.95),
                results.stream().mapToInt(AnswerEvaluationCaseResult::promptTokens).sum(),
                results.stream().mapToInt(AnswerEvaluationCaseResult::completionTokens).sum(),
                results.stream().mapToInt(AnswerEvaluationCaseResult::totalTokens).sum(),
                List.copyOf(results));
    }

    private long correctCitationCount(AnswerEvaluationCaseResult result) { // 方法作用：完成“correctCitationCount”对应的内部步骤，供主流程复用。
        Set<String> expected = new HashSet<>(result.expectedHeadingPaths());
        return result.citedHeadingPaths().stream().filter(expected::contains).count();
    }

    private boolean matchesAny(String answer, List<String> alternatives) { // 方法作用：完成“matchesAny”对应的内部步骤，供主流程复用。
        String normalizedAnswer = normalize(answer);
        return alternatives.stream().map(this::normalize).anyMatch(normalizedAnswer::contains);
    }

    private double faithfulnessProxy(String answer, List<RagCitation> citations) { // 方法作用：完成“faithfulnessProxy”对应的内部步骤，供主流程复用。
        String normalizedAnswer = normalize(CITATION.matcher(answer).replaceAll(""));
        Set<String> answerBigrams = bigrams(normalizedAnswer);
        if (answerBigrams.isEmpty()) {
            return 0;
        }
        String evidence = citations.stream().map(RagCitation::excerpt).reduce("", String::concat);
        Set<String> evidenceBigrams = bigrams(normalize(evidence));
        long supported = answerBigrams.stream().filter(evidenceBigrams::contains).count();
        return supported / (double) answerBigrams.size();
    }

    private Set<String> bigrams(String text) { // 方法作用：完成“bigrams”对应的内部步骤，供主流程复用。
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int index = 0; index + 1 < text.length(); index++) {
            result.add(text.substring(index, index + 2));
        }
        return result;
    }

    private String normalize(String value) { // 方法作用：完成“normalize”对应的内部步骤，供主流程复用。
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return NON_CONTENT.matcher(normalized).replaceAll("");
    }

    private int value(Integer value) { // 方法作用：完成“value”对应的内部步骤，供主流程复用。
        return value == null ? 0 : value;
    }

    private double divide(long numerator, long denominator) { // 方法作用：完成“divide”对应的内部步骤，供主流程复用。
        return denominator == 0 ? 0 : numerator / (double) denominator;
    }

    private double percentile(List<Double> sortedValues, double percentile) { // 方法作用：完成“percentile”对应的内部步骤，供主流程复用。
        int index = Math.max(0, (int) Math.ceil(percentile * sortedValues.size()) - 1);
        return sortedValues.get(index);
    }
}
