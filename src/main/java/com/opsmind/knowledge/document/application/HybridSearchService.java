package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.knowledge.document.config.EmbeddingProperties;
import com.opsmind.knowledge.document.config.HybridSearchProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 混合检索服务：合并向量语义检索和 BM25 关键词检索。
 * <p>向量检索擅长“意思相近”，BM25 擅长错误码、产品名等精确词；RRF（倒数排名融合）
 * 只使用各自名次，不要求两种分数处于相同量纲。</p>
 */
@Service // 注解作用：把类注册为业务服务，由 Spring 负责创建和注入。
public class HybridSearchService { // 类型作用：应用服务，负责组织一个完整业务流程。

    private final SemanticSearchService vectorSearchService;
    private final Bm25SearchService lexicalSearchService;
    private final EmbeddingProperties embeddingProperties;
    private final HybridSearchProperties hybridProperties;

    public HybridSearchService(
            SemanticSearchService vectorSearchService,
            Bm25SearchService lexicalSearchService,
            EmbeddingProperties embeddingProperties,
            HybridSearchProperties hybridProperties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.vectorSearchService = vectorSearchService;
        this.lexicalSearchService = lexicalSearchService;
        this.embeddingProperties = embeddingProperties;
        this.hybridProperties = hybridProperties;
    }

    public SemanticSearchResult search(String query, Integer requestedTopK, UUID documentId) { // 方法作用：执行检索或相关性排序并返回有序结果。
        int topK = requestedTopK == null ? embeddingProperties.defaultTopK() : requestedTopK;
        if (topK < 1 || topK > embeddingProperties.maxTopK()) {
            throw new DocumentException(
                    "SEARCH_TOP_K_INVALID",
                    "topK must be between 1 and " + embeddingProperties.maxTopK(),
                    HttpStatus.BAD_REQUEST);
        }
        // 先多召回一些候选，再融合并截取 topK；候选太少会让后续重排没有选择空间。
        int candidateLimit = Math.min(
                embeddingProperties.maxTopK(),
                Math.max(hybridProperties.minimumCandidates(), topK * hybridProperties.candidateMultiplier()));
        SemanticSearchResult vectorResult = vectorSearchService.search(query, candidateLimit, documentId);
        SemanticSearchResult lexicalResult = lexicalSearchService.search(query, documentId, candidateLimit);

        Map<UUID, FusedHit> fused = new HashMap<>();
        addRanking(fused, vectorResult.hits());
        addRanking(fused, lexicalResult.hits());
        List<SemanticSearchHit> hits = fused.values().stream()
                .sorted((left, right) -> {
                    int scoreOrder = Double.compare(right.score(), left.score());
                    return scoreOrder != 0 ? scoreOrder : left.hit().chunkId().compareTo(right.hit().chunkId());
                })
                .limit(topK)
                .map(item -> withScore(item.hit(), item.score()))
                .toList();
        return new SemanticSearchResult(
                vectorResult.query(),
                "hybrid-rrf[" + vectorResult.model() + "+" + Bm25SearchService.MODEL + "]",
                documentId,
                Math.max(vectorResult.candidateCount(), lexicalResult.candidateCount()),
                hits);
    }

    private void addRanking(Map<UUID, FusedHit> fused, List<SemanticSearchHit> ranking) { // 方法作用：完成“addRanking”对应的内部步骤，供主流程复用。
        for (int index = 0; index < ranking.size(); index++) {
            SemanticSearchHit hit = ranking.get(index);
            // RRF 公式：1 / (k + rank)。同一块被两路同时命中时分数相加，会自然排到前面。
            double reciprocalRank = 1.0 / (hybridProperties.rrfK() + index + 1);
            fused.compute(hit.chunkId(), (chunkId, existing) -> existing == null
                    ? new FusedHit(hit, reciprocalRank)
                    : new FusedHit(existing.hit(), existing.score() + reciprocalRank));
        }
    }

    private SemanticSearchHit withScore(SemanticSearchHit hit, double score) { // 方法作用：完成“withScore”对应的内部步骤，供主流程复用。
        return new SemanticSearchHit(
                hit.chunkId(), hit.parentId(), hit.documentId(), hit.filename(), hit.headingPath(),
                hit.childText(), hit.parentText(), score);
    }

    private record FusedHit(SemanticSearchHit hit /* 字段作用：该不可变对象携带的 hit 数据。 */, double score /* 字段作用：相关性或质量分数，含义由当前算法定义。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
    }
}
