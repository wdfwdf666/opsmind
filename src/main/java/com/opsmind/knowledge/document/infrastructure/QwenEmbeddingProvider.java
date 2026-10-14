package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

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

@Component // 注解作用：把普通组件交给 Spring 容器管理。
@ConditionalOnProperty(prefix = "opsmind.embedding", name = "provider", havingValue = "qwen", matchIfMissing = true) // 注解作用：只有配置值满足条件时才启用当前实现。
public class QwenEmbeddingProvider implements EmbeddingProvider { // 类型作用：Qwen 适配器，负责请求组装、远程调用和响应校验。

    private final EmbeddingProperties properties;
    private final RestClient restClient;

    @Autowired // 注解作用：要求 Spring 按构造器参数类型注入依赖。
    public QwenEmbeddingProvider(EmbeddingProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this(properties, RestClient.builder());
    }

    QwenEmbeddingProvider(EmbeddingProperties properties, RestClient.Builder restClientBuilder) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public String model() { // 方法作用：返回当前实现使用的模型名称，供响应和审计展示。
        return properties.model();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public int dimension() { // 方法作用：完成“dimension”对应的内部步骤，供主流程复用。
        return properties.dimension();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public List<double[]> embed(List<String> texts) { // 方法作用：完成“embed”对应的内部步骤，供主流程复用。
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

    private List<double[]> requestBatch(List<String> texts) { // 方法作用：完成“requestBatch”对应的内部步骤，供主流程复用。
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

    private List<double[]> vectors(QwenEmbeddingResponse response, int expectedCount) { // 方法作用：完成“vectors”对应的内部步骤，供主流程复用。
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

    private void validateConfiguration() { // 方法作用：在发起远程调用前检查地址、密钥等必填配置。
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()
                || properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new DocumentException(
                    "EMBEDDING_CONFIGURATION_MISSING",
                    "Qwen embedding requires QWEN_BASE_URL and DASHSCOPE_API_KEY",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private String endpoint() { // 方法作用：规范化基础地址并拼出真正调用的远程接口地址。
        return properties.baseUrl().replaceAll("/+$", "") + "/embeddings";
    }

    private DocumentException invalidResponse(String reason) { // 方法作用：完成“invalidResponse”对应的内部步骤，供主流程复用。
        return new DocumentException(
                "EMBEDDING_PROVIDER_INVALID_RESPONSE",
                "Qwen embedding returned an invalid response: " + reason,
                HttpStatus.BAD_GATEWAY);
    }

    record QwenEmbeddingRequest(
            String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */,
            List<String> input /* 字段作用：该不可变对象携带的 input 数据。 */,
            int dimensions /* 字段作用：该不可变对象携带的 dimensions 数据。 */,
            @JsonProperty("encoding_format") String encodingFormat /* 字段作用：该不可变对象携带的 encodingFormat 数据。 */) { // 类型作用：请求 DTO，规定调用接口时可以提交哪些 JSON 字段。
    }

    record QwenEmbeddingResponse(List<QwenEmbeddingData> data /* 字段作用：该不可变对象携带的 data 数据。 */, String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */, QwenUsage usage /* 字段作用：该不可变对象携带的 usage 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。
    }

    record QwenEmbeddingData(int index /* 字段作用：该不可变对象携带的 index 数据。 */, List<Double> embedding /* 字段作用：该不可变对象携带的 embedding 数据。 */) { // 类型作用：Qwen 适配器，负责请求组装、远程调用和响应校验。
    }

    record QwenUsage(
            @JsonProperty("prompt_tokens") Integer promptTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
            @JsonProperty("total_tokens") Integer totalTokens /* 字段作用：模型输入或输出的 Token 数量。 */) { // 类型作用：Qwen 适配器，负责请求组装、远程调用和响应校验。
    }
}
