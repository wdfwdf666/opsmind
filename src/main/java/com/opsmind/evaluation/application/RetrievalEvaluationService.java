package com.opsmind.evaluation.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.evaluation.domain.RetrievalEvaluationCase;
import com.opsmind.evaluation.domain.RetrievalEvaluationCaseResult;
import com.opsmind.evaluation.domain.RetrievalEvaluationDataset;
import com.opsmind.evaluation.domain.RetrievalEvaluationReport;
import com.opsmind.evaluation.domain.RetrievalMetricAtK;
import com.opsmind.evaluation.domain.RetrievalStrategy;
import com.opsmind.knowledge.document.application.HybridSearchService;
import com.opsmind.knowledge.document.application.RerankedSearchService;
import com.opsmind.knowledge.document.application.SemanticSearchService;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * 检索离线评测服务。
 * <p>对固定问题集运行 VECTOR/HYBRID/RERANKED，计算 Recall@K、MRR@K、HitRate@K
 * 与延迟分位数，用客观指标判断一次算法修改是否真的提升效果。</p>
 */
@Service // 注解作用：把类注册为业务服务，由 Spring 负责创建和注入。
public class RetrievalEvaluationService { // 类型作用：应用服务，负责组织一个完整业务流程。

    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    private final RetrievalEvaluationDatasetLoader datasetLoader;
    private final SemanticSearchService searchService;
    private final HybridSearchService hybridSearchService;
    private final RerankedSearchService rerankedSearchService;
    private final LongSupplier nanoTime;

    @Autowired // 注解作用：要求 Spring 按构造器参数类型注入依赖。
    public RetrievalEvaluationService(
            RetrievalEvaluationDatasetLoader datasetLoader,
            SemanticSearchService searchService,
            HybridSearchService hybridSearchService,
            RerankedSearchService rerankedSearchService) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this(datasetLoader, searchService, hybridSearchService, rerankedSearchService, System::nanoTime);
    }

    // 测试可传入假的 nanoTime，消除真实机器速度波动对断言的影响。
    RetrievalEvaluationService(
            RetrievalEvaluationDatasetLoader datasetLoader,
            SemanticSearchService searchService,
            HybridSearchService hybridSearchService,
            RerankedSearchService rerankedSearchService,
            LongSupplier nanoTime) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.datasetLoader = datasetLoader;
        this.searchService = searchService;
        this.hybridSearchService = hybridSearchService;
        this.rerankedSearchService = rerankedSearchService;
        this.nanoTime = nanoTime;
    }

    public RetrievalEvaluationReport run(String datasetName, UUID documentId, RetrievalStrategy requestedStrategy) { // 方法作用：加载固定评测集，逐条执行并汇总质量与延迟指标。
        RetrievalStrategy strategy = requestedStrategy == null ? RetrievalStrategy.VECTOR : requestedStrategy;
        RetrievalEvaluationDataset dataset = datasetLoader.load(datasetName);
        int maxK = dataset.cutoffs().stream().mapToInt(Integer::intValue).max().orElseThrow();
        List<RetrievalEvaluationCaseResult> caseResults = new ArrayList<>(dataset.cases().size());
        String model = null;
        // 每条 case 都有问题和期望标题路径，实际命中结果会保留到报告中便于逐条排错。
        for (RetrievalEvaluationCase evaluationCase : dataset.cases()) {
            long startedAt = nanoTime.getAsLong();
            SemanticSearchResult searchResult = search(strategy, evaluationCase.query(), maxK, documentId);
            double latencyMs = (nanoTime.getAsLong() - startedAt) / NANOS_PER_MILLISECOND;
            if (model == null) {
                model = searchResult.model();
            }
            caseResults.add(toCaseResult(evaluationCase, searchResult, latencyMs));
        }
        List<Double> latencies = caseResults.stream().map(RetrievalEvaluationCaseResult::latencyMs).sorted().toList();
        return new RetrievalEvaluationReport(
                dataset.name(), dataset.description(), strategy, model, documentId, caseResults.size(), maxK,
                caseResults.stream().mapToInt(RetrievalEvaluationCaseResult::candidateCount).average().orElse(0),
                percentile(latencies, 0.50), percentile(latencies, 0.95),
                dataset.cutoffs().stream().sorted().map(k -> metricAtK(caseResults, k)).toList(),
                List.copyOf(caseResults));
    }

    private SemanticSearchResult search(RetrievalStrategy strategy, String query, int topK, UUID documentId) { // 方法作用：执行检索或相关性排序并返回有序结果。
        return switch (strategy) {
            case VECTOR -> searchService.search(query, topK, documentId);
            case HYBRID -> hybridSearchService.search(query, topK, documentId);
            case RERANKED -> rerankedSearchService.search(query, topK, documentId);
        };
    }

    private RetrievalEvaluationCaseResult toCaseResult(
            RetrievalEvaluationCase evaluationCase, SemanticSearchResult searchResult, double latencyMs) { // 方法作用：把外部或数据库数据转换成项目内部对象。
        List<String> headings = searchResult.hits().stream().map(SemanticSearchHit::headingPath).toList();
        Set<String> expected = new HashSet<>(evaluationCase.expectedHeadingPaths());
        Integer firstRelevantRank = null;
        for (int index = 0; index < headings.size(); index++) {
            if (expected.contains(headings.get(index))) {
                firstRelevantRank = index + 1;
                break;
            }
        }
        return new RetrievalEvaluationCaseResult(
                evaluationCase.id(), evaluationCase.query(), List.copyOf(evaluationCase.expectedHeadingPaths()),
                headings, firstRelevantRank, searchResult.candidateCount(), latencyMs);
    }

    private RetrievalMetricAtK metricAtK(List<RetrievalEvaluationCaseResult> cases, int k) { // 方法作用：完成“metricAtK”对应的内部步骤，供主流程复用。
        // Recall 衡量相关结果找回多少；MRR 重视第一个相关结果的位置；HitRate 衡量是否至少命中一个。
        double recall = cases.stream().mapToDouble(result -> recallAtK(result, k)).average().orElse(0);
        double mrr = cases.stream().mapToDouble(result -> reciprocalRankAtK(result, k)).average().orElse(0);
        double hitRate = cases.stream().filter(result -> isHitAtK(result, k)).count() / (double) cases.size();
        return new RetrievalMetricAtK(k, recall, mrr, hitRate);
    }

    private double recallAtK(RetrievalEvaluationCaseResult result, int k) { // 方法作用：完成“recallAtK”对应的内部步骤，供主流程复用。
        Set<String> expected = new HashSet<>(result.expectedHeadingPaths());
        long relevantRetrieved = result.retrievedHeadingPaths().stream().limit(k).distinct()
                .filter(expected::contains).count();
        return relevantRetrieved / (double) expected.size();
    }

    private double reciprocalRankAtK(RetrievalEvaluationCaseResult result, int k) { // 方法作用：完成“reciprocalRankAtK”对应的内部步骤，供主流程复用。
        return isHitAtK(result, k) ? 1.0 / result.firstRelevantRank() : 0;
    }

    private boolean isHitAtK(RetrievalEvaluationCaseResult result, int k) { // 方法作用：判断当前数据或状态是否满足指定条件。
        return result.firstRelevantRank() != null && result.firstRelevantRank() <= k;
    }

    private double percentile(List<Double> sortedValues, double percentile) { // 方法作用：完成“percentile”对应的内部步骤，供主流程复用。
        int index = Math.max(0, (int) Math.ceil(percentile * sortedValues.size()) - 1);
        return sortedValues.get(index);
    }
}
