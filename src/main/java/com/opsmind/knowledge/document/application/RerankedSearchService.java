package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.config.EmbeddingProperties;
import com.opsmind.knowledge.document.config.RerankingProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 检索最终阶段：先用混合检索快速召回，再让 Reranker 对候选做更精确的相关性排序。
 * <p>这是典型的“两阶段检索”：召回阶段追求不遗漏，重排阶段追求前几名准确。</p>
 */
@Service
public class RerankedSearchService {

    private final HybridSearchService hybridSearchService;
    private final RerankingProvider rerankingProvider;
    private final EmbeddingProperties embeddingProperties;
    private final RerankingProperties rerankingProperties;

    public RerankedSearchService(
            HybridSearchService hybridSearchService,
            RerankingProvider rerankingProvider,
            EmbeddingProperties embeddingProperties,
            RerankingProperties rerankingProperties) {
        this.hybridSearchService = hybridSearchService;
        this.rerankingProvider = rerankingProvider;
        this.embeddingProperties = embeddingProperties;
        this.rerankingProperties = rerankingProperties;
    }

    public SemanticSearchResult search(String query, Integer requestedTopK, UUID documentId) {
        int topK = requestedTopK == null ? embeddingProperties.defaultTopK() : requestedTopK;
        if (topK < 1 || topK > embeddingProperties.maxTopK()) {
            throw new DocumentException(
                    "SEARCH_TOP_K_INVALID",
                    "topK must be between 1 and " + embeddingProperties.maxTopK(),
                    HttpStatus.BAD_REQUEST);
        }
        int candidateLimit = Math.min(
                embeddingProperties.maxTopK(),
                Math.max(rerankingProperties.minimumCandidates(), topK * rerankingProperties.candidateMultiplier()));
        SemanticSearchResult hybridResult = hybridSearchService.search(query, candidateLimit, documentId);
        // Provider 是可替换接口：可配置 Qwen 重排，也可在本地使用词法重排实现。
        List<SemanticSearchHit> reranked = rerankingProvider.rerank(
                hybridResult.query(), hybridResult.hits(), topK);
        return new SemanticSearchResult(
                hybridResult.query(),
                hybridResult.model() + "->rerank[" + rerankingProvider.model() + "]",
                documentId,
                hybridResult.candidateCount(),
                reranked);
    }
}
