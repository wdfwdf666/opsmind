package com.opsmind.knowledge.document.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsmind.knowledge.document.application.RerankingProvider;
import com.opsmind.knowledge.document.config.RerankingProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "opsmind.reranking", name = "provider", havingValue = "qwen", matchIfMissing = true)
public class QwenRerankingProvider implements RerankingProvider {

    private final RerankingProperties properties;
    private final RestClient restClient;

    @Autowired
    public QwenRerankingProvider(RerankingProperties properties) {
        this(properties, RestClient.builder());
    }

    QwenRerankingProvider(RerankingProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public List<SemanticSearchHit> rerank(String query, List<SemanticSearchHit> candidates, int topK) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        validateConfiguration();
        List<String> documents = candidates.stream().map(this::documentText).toList();
        QwenRerankRequest request = new QwenRerankRequest(
                model(), new QwenRerankInput(query, documents),
                new QwenRerankParameters(Math.min(topK, candidates.size()), properties.instruction()));
        try {
            QwenRerankResponse response = restClient.post()
                    .uri(properties.endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .body(request)
                    .retrieve()
                    .body(QwenRerankResponse.class);
            return mapResults(response, candidates, Math.min(topK, candidates.size()));
        } catch (RestClientResponseException exception) {
            throw new DocumentException(
                    "RERANKING_PROVIDER_ERROR",
                    "Qwen reranking request failed with HTTP " + exception.getStatusCode().value(),
                    HttpStatus.BAD_GATEWAY);
        } catch (RestClientException exception) {
            throw new DocumentException(
                    "RERANKING_PROVIDER_UNAVAILABLE",
                    "Qwen reranking service is unavailable",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private List<SemanticSearchHit> mapResults(
            QwenRerankResponse response, List<SemanticSearchHit> candidates, int expectedCount) {
        if (response == null || response.output() == null || response.output().results() == null
                || response.output().results().size() != expectedCount) {
            throw invalidResponse("unexpected result count");
        }
        Set<Integer> seenIndexes = new HashSet<>();
        return response.output().results().stream()
                .peek(result -> validateResult(result, candidates.size(), seenIndexes))
                .sorted((left, right) -> Double.compare(right.relevanceScore(), left.relevanceScore()))
                .map(result -> withScore(candidates.get(result.index()), result.relevanceScore()))
                .toList();
    }

    private void validateResult(QwenRerankResult result, int candidateCount, Set<Integer> seenIndexes) {
        if (result.index() < 0 || result.index() >= candidateCount
                || !Double.isFinite(result.relevanceScore())
                || result.relevanceScore() < 0 || result.relevanceScore() > 1
                || !seenIndexes.add(result.index())) {
            throw invalidResponse("invalid index or relevance score");
        }
    }

    private String documentText(SemanticSearchHit hit) {
        return (hit.headingPath() == null ? "" : hit.headingPath()) + "\n\n" + hit.childText();
    }

    private SemanticSearchHit withScore(SemanticSearchHit hit, double score) {
        return new SemanticSearchHit(
                hit.chunkId(), hit.parentId(), hit.documentId(), hit.filename(), hit.headingPath(),
                hit.childText(), hit.parentText(), score);
    }

    private void validateConfiguration() {
        if (properties.endpoint() == null || properties.endpoint().isBlank()
                || properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new DocumentException(
                    "RERANKING_CONFIGURATION_MISSING",
                    "Qwen reranking requires QWEN_RERANK_URL and DASHSCOPE_API_KEY",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private DocumentException invalidResponse(String reason) {
        return new DocumentException(
                "RERANKING_PROVIDER_INVALID_RESPONSE",
                "Qwen reranking returned an invalid response: " + reason,
                HttpStatus.BAD_GATEWAY);
    }

    record QwenRerankRequest(String model, QwenRerankInput input, QwenRerankParameters parameters) {
    }

    record QwenRerankInput(String query, List<String> documents) {
    }

    record QwenRerankParameters(
            @JsonProperty("top_n") int topN,
            String instruct) {
    }

    record QwenRerankResponse(QwenRerankOutput output, QwenRerankUsage usage,
                              @JsonProperty("request_id") String requestId) {
    }

    record QwenRerankOutput(List<QwenRerankResult> results) {
    }

    record QwenRerankResult(int index, @JsonProperty("relevance_score") double relevanceScore) {
    }

    record QwenRerankUsage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {
    }
}
