package com.opsmind.knowledge.document.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsmind.knowledge.document.application.EmbeddingProvider;
import com.opsmind.knowledge.document.config.EmbeddingProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "opsmind.embedding", name = "provider", havingValue = "qwen", matchIfMissing = true)
public class QwenEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingProperties properties;
    private final RestClient restClient;

    @Autowired
    public QwenEmbeddingProvider(EmbeddingProperties properties) {
        this(properties, RestClient.builder());
    }

    QwenEmbeddingProvider(EmbeddingProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public int dimension() {
        return properties.dimension();
    }

    @Override
    public List<double[]> embed(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        validateConfiguration();
        List<double[]> result = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += properties.batchSize()) {
            int end = Math.min(start + properties.batchSize(), texts.size());
            result.addAll(requestBatch(texts.subList(start, end)));
        }
        return List.copyOf(result);
    }

    private List<double[]> requestBatch(List<String> texts) {
        QwenEmbeddingRequest request = new QwenEmbeddingRequest(
                model(), texts, dimension(), "float");
        try {
            QwenEmbeddingResponse response = restClient.post()
                    .uri(endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .body(request)
                    .retrieve()
                    .body(QwenEmbeddingResponse.class);
            return vectors(response, texts.size());
        } catch (RestClientResponseException exception) {
            throw new DocumentException(
                    "EMBEDDING_PROVIDER_ERROR",
                    "Qwen embedding request failed with HTTP " + exception.getStatusCode().value(),
                    HttpStatus.BAD_GATEWAY);
        } catch (RestClientException exception) {
            throw new DocumentException(
                    "EMBEDDING_PROVIDER_UNAVAILABLE",
                    "Qwen embedding service is unavailable",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private List<double[]> vectors(QwenEmbeddingResponse response, int expectedCount) {
        if (response == null || response.data() == null || response.data().size() != expectedCount) {
            throw invalidResponse("unexpected vector count");
        }
        List<QwenEmbeddingData> ordered = response.data().stream()
                .sorted(Comparator.comparingInt(QwenEmbeddingData::index))
                .toList();
        List<double[]> vectors = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            QwenEmbeddingData item = ordered.get(index);
            if (item.index() != index || item.embedding() == null || item.embedding().size() != dimension()) {
                throw invalidResponse("unexpected index or dimension");
            }
            vectors.add(item.embedding().stream().mapToDouble(Double::doubleValue).toArray());
        }
        return vectors;
    }

    private void validateConfiguration() {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()
                || properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new DocumentException(
                    "EMBEDDING_CONFIGURATION_MISSING",
                    "Qwen embedding requires QWEN_BASE_URL and DASHSCOPE_API_KEY",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private String endpoint() {
        return properties.baseUrl().replaceAll("/+$", "") + "/embeddings";
    }

    private DocumentException invalidResponse(String reason) {
        return new DocumentException(
                "EMBEDDING_PROVIDER_INVALID_RESPONSE",
                "Qwen embedding returned an invalid response: " + reason,
                HttpStatus.BAD_GATEWAY);
    }

    record QwenEmbeddingRequest(
            String model,
            List<String> input,
            int dimensions,
            @JsonProperty("encoding_format") String encodingFormat) {
    }

    record QwenEmbeddingResponse(List<QwenEmbeddingData> data, String model, QwenUsage usage) {
    }

    record QwenEmbeddingData(int index, List<Double> embedding) {
    }

    record QwenUsage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {
    }
}
