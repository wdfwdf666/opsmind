package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

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

@Service // 注解作用：把类注册为业务服务，由 Spring 负责创建和注入。
public class Bm25SearchService { // 类型作用：应用服务，负责组织一个完整业务流程。

    public static final String MODEL = "bm25-char-ngram-v1";
    private static final int MAX_QUERY_CHARACTERS = 1000;
    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private final LexicalChunkRepository chunkRepository;
    private final ChineseNgramTokenizer tokenizer;

    public Bm25SearchService(LexicalChunkRepository chunkRepository, ChineseNgramTokenizer tokenizer) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.chunkRepository = chunkRepository;
        this.tokenizer = tokenizer;
    }

    public SemanticSearchResult search(String query, UUID documentId, int limit) { // 方法作用：执行检索或相关性排序并返回有序结果。
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

    private Map<String, Integer> documentFrequencies(List<TokenizedChunk> chunks, Set<String> queryTerms) { // 方法作用：完成“documentFrequencies”对应的内部步骤，供主流程复用。
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
            double averageLength) { // 方法作用：完成“score”对应的内部步骤，供主流程复用。
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

    private String searchText(SearchableChunk chunk) { // 方法作用：执行检索或相关性排序并返回有序结果。
        return (chunk.headingPath() == null ? "" : chunk.headingPath()) + "\n" + chunk.childText();
    }

    private String validateQuery(String query) { // 方法作用：完成“validateQuery”对应的内部步骤，供主流程复用。
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

    private SemanticSearchHit toHit(ScoredChunk scoredChunk) { // 方法作用：把外部或数据库数据转换成项目内部对象。
        SearchableChunk chunk = scoredChunk.chunk();
        return new SemanticSearchHit(
                chunk.chunkId(), chunk.parentId(), chunk.documentId(), chunk.filename(), chunk.headingPath(),
                chunk.childText(), chunk.parentText(), scoredChunk.score());
    }

    private record TokenizedChunk(SearchableChunk chunk /* 字段作用：该不可变对象携带的 chunk 数据。 */, List<String> tokens /* 字段作用：模型输入或输出的 Token 数量。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
    }

    private record ScoredChunk(SearchableChunk chunk /* 字段作用：该不可变对象携带的 chunk 数据。 */, double score /* 字段作用：相关性或质量分数，含义由当前算法定义。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
    }
}
