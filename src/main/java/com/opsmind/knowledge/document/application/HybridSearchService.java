package com.opsmind.knowledge.document.application;

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

@Service
public class HybridSearchService {

    private final SemanticSearchService vectorSearchService;
    private final Bm25SearchService lexicalSearchService;
    private final EmbeddingProperties embeddingProperties;
    private final HybridSearchProperties hybridProperties;

    public HybridSearchService(
            SemanticSearchService vectorSearchService,
            Bm25SearchService lexicalSearchService,
            EmbeddingProperties embeddingProperties,
            HybridSearchProperties hybridProperties) {
        this.vectorSearchService = vectorSearchService;
        this.lexicalSearchService = lexicalSearchService;
        this.embeddingProperties = embeddingProperties;
        this.hybridProperties = hybridProperties;
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

    private void addRanking(Map<UUID, FusedHit> fused, List<SemanticSearchHit> ranking) {
        for (int index = 0; index < ranking.size(); index++) {
            SemanticSearchHit hit = ranking.get(index);
            double reciprocalRank = 1.0 / (hybridProperties.rrfK() + index + 1);
            fused.compute(hit.chunkId(), (chunkId, existing) -> existing == null
                    ? new FusedHit(hit, reciprocalRank)
                    : new FusedHit(existing.hit(), existing.score() + reciprocalRank));
        }
    }

    private SemanticSearchHit withScore(SemanticSearchHit hit, double score) {
        return new SemanticSearchHit(
                hit.chunkId(), hit.parentId(), hit.documentId(), hit.filename(), hit.headingPath(),
                hit.childText(), hit.parentText(), score);
    }

    private record FusedHit(SemanticSearchHit hit, double score) {
    }
}
