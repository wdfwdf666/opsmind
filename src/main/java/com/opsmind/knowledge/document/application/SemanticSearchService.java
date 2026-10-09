package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.config.EmbeddingProperties;
import com.opsmind.knowledge.document.domain.ChunkEmbeddingRepository;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.NearestChunkSearch;
import com.opsmind.knowledge.document.domain.ScoredEmbeddedChunk;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SemanticSearchService {

    private static final int MAX_QUERY_CHARACTERS = 1000;

    private final EmbeddingProvider embeddingProvider;
    private final ChunkEmbeddingRepository embeddingRepository;
    private final EmbeddingProperties properties;

    public SemanticSearchService(
            EmbeddingProvider embeddingProvider,
            ChunkEmbeddingRepository embeddingRepository,
            EmbeddingProperties properties) {
        this.embeddingProvider = embeddingProvider;
        this.embeddingRepository = embeddingRepository;
        this.properties = properties;
    }

    public SemanticSearchResult search(String query, Integer requestedTopK, UUID documentId) {
        String normalizedQuery = validateQuery(query);
        int topK = requestedTopK == null ? properties.defaultTopK() : requestedTopK;
        if (topK < 1 || topK > properties.maxTopK()) {
            throw new DocumentException(
                    "SEARCH_TOP_K_INVALID",
                    "topK must be between 1 and " + properties.maxTopK(),
                    HttpStatus.BAD_REQUEST);
        }
        double[] queryVector = embeddingProvider.embed(List.of(normalizedQuery)).getFirst();
        NearestChunkSearch nearest = embeddingRepository.findNearest(
                embeddingProvider.model(), queryVector, documentId, topK);
        List<SemanticSearchHit> hits = nearest.hits().stream().map(this::toHit).toList();
        return new SemanticSearchResult(
                normalizedQuery, embeddingProvider.model(), documentId, nearest.candidateCount(), hits);
    }

    private String validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new DocumentException("SEARCH_QUERY_EMPTY", "Search query must not be blank", HttpStatus.BAD_REQUEST);
        }
        String normalized = query.strip();
        if (normalized.length() > MAX_QUERY_CHARACTERS) {
            throw new DocumentException(
                    "SEARCH_QUERY_TOO_LONG",
                    "Search query must not exceed " + MAX_QUERY_CHARACTERS + " characters",
                    HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private SemanticSearchHit toHit(ScoredEmbeddedChunk candidate) {
        return new SemanticSearchHit(
                candidate.chunkId(), candidate.parentId(), candidate.documentId(), candidate.filename(),
                candidate.headingPath(), candidate.childText(), candidate.parentText(), candidate.score());
    }
}
