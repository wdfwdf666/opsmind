package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.LexicalChunkRepository;
import com.opsmind.knowledge.document.domain.SearchableChunk;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class Bm25SearchService {

    public static final String MODEL = "bm25-char-ngram-v1";
    private static final int MAX_QUERY_CHARACTERS = 1000;
    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private final LexicalChunkRepository chunkRepository;
    private final ChineseNgramTokenizer tokenizer;

    public Bm25SearchService(LexicalChunkRepository chunkRepository, ChineseNgramTokenizer tokenizer) {
        this.chunkRepository = chunkRepository;
        this.tokenizer = tokenizer;
    }

    public SemanticSearchResult search(String query, UUID documentId, int limit) {
        String normalizedQuery = validateQuery(query);
        if (limit < 1) {
            throw new DocumentException("SEARCH_TOP_K_INVALID", "Search limit must be positive", HttpStatus.BAD_REQUEST);
        }
        List<SearchableChunk> candidates = chunkRepository.findCandidates(documentId);
        if (candidates.isEmpty()) {
            return new SemanticSearchResult(normalizedQuery, MODEL, documentId, 0, List.of());
        }

        List<TokenizedChunk> tokenizedChunks = candidates.stream()
                .map(chunk -> new TokenizedChunk(chunk, tokenizer.tokenize(searchText(chunk))))
                .toList();
        double averageLength = tokenizedChunks.stream().mapToInt(chunk -> chunk.tokens().size()).average().orElse(1);
        Set<String> queryTerms = new HashSet<>(tokenizer.tokenize(normalizedQuery));
        Map<String, Integer> documentFrequencies = documentFrequencies(tokenizedChunks, queryTerms);

        List<ScoredChunk> scored = new ArrayList<>(tokenizedChunks.size());
        for (TokenizedChunk tokenizedChunk : tokenizedChunks) {
            double score = score(tokenizedChunk.tokens(), queryTerms, documentFrequencies,
                    tokenizedChunks.size(), averageLength);
            scored.add(new ScoredChunk(tokenizedChunk.chunk(), score));
        }
        List<SemanticSearchHit> hits = scored.stream()
                .filter(candidate -> candidate.score() > 0)
                .sorted((left, right) -> {
                    int scoreOrder = Double.compare(right.score(), left.score());
                    return scoreOrder != 0 ? scoreOrder : left.chunk().chunkId().compareTo(right.chunk().chunkId());
                })
                .limit(limit)
                .map(this::toHit)
                .toList();
        return new SemanticSearchResult(normalizedQuery, MODEL, documentId, candidates.size(), hits);
    }

    private Map<String, Integer> documentFrequencies(List<TokenizedChunk> chunks, Set<String> queryTerms) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (TokenizedChunk chunk : chunks) {
            Set<String> uniqueTokens = new HashSet<>(chunk.tokens());
            for (String term : queryTerms) {
                if (uniqueTokens.contains(term)) {
                    frequencies.merge(term, 1, Integer::sum);
                }
            }
        }
        return frequencies;
    }

    private double score(
            List<String> tokens,
            Set<String> queryTerms,
            Map<String, Integer> documentFrequencies,
            int documentCount,
            double averageLength) {
        Map<String, Integer> termFrequencies = new HashMap<>();
        tokens.forEach(token -> termFrequencies.merge(token, 1, Integer::sum));
        double score = 0;
        for (String term : queryTerms) {
            int termFrequency = termFrequencies.getOrDefault(term, 0);
            if (termFrequency == 0) {
                continue;
            }
            int documentFrequency = documentFrequencies.getOrDefault(term, 0);
            double inverseDocumentFrequency = Math.log(1
                    + (documentCount - documentFrequency + 0.5) / (documentFrequency + 0.5));
            double lengthNormalization = K1 * (1 - B + B * tokens.size() / averageLength);
            score += inverseDocumentFrequency * termFrequency * (K1 + 1)
                    / (termFrequency + lengthNormalization);
        }
        return score;
    }

    private String searchText(SearchableChunk chunk) {
        return (chunk.headingPath() == null ? "" : chunk.headingPath()) + "\n" + chunk.childText();
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

    private SemanticSearchHit toHit(ScoredChunk scoredChunk) {
        SearchableChunk chunk = scoredChunk.chunk();
        return new SemanticSearchHit(
                chunk.chunkId(), chunk.parentId(), chunk.documentId(), chunk.filename(), chunk.headingPath(),
                chunk.childText(), chunk.parentText(), scoredChunk.score());
    }

    private record TokenizedChunk(SearchableChunk chunk, List<String> tokens) {
    }

    private record ScoredChunk(SearchableChunk chunk, double score) {
    }
}
