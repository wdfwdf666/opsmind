package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

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

@Component // 注解作用：把普通组件交给 Spring 容器管理。
@ConditionalOnProperty(prefix = "opsmind.reranking", name = "provider", havingValue = "qwen", matchIfMissing = true) // 注解作用：只有配置值满足条件时才启用当前实现。
public class QwenRerankingProvider implements RerankingProvider { // 类型作用：Qwen 适配器，负责请求组装、远程调用和响应校验。

    private final RerankingProperties properties;
    private final RestClient restClient;

    @Autowired // 注解作用：要求 Spring 按构造器参数类型注入依赖。
    public QwenRerankingProvider(RerankingProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this(properties, RestClient.builder());
    }

    QwenRerankingProvider(RerankingProperties properties, RestClient.Builder restClientBuilder) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public String model() { // 方法作用：返回当前实现使用的模型名称，供响应和审计展示。
        return properties.model();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public List<SemanticSearchHit> rerank(String query, List<SemanticSearchHit> candidates, int topK) { // 方法作用：执行检索或相关性排序并返回有序结果。
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
            QwenRerankResponse response, List<SemanticSearchHit> candidates, int expectedCount) { // 方法作用：把外部或数据库数据转换成项目内部对象。
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

    private void validateResult(QwenRerankResult result, int candidateCount, Set<Integer> seenIndexes) { // 方法作用：完成“validateResult”对应的内部步骤，供主流程复用。
        if (result.index() < 0 || result.index() >= candidateCount
                || !Double.isFinite(result.relevanceScore())
                || result.relevanceScore() < 0 || result.relevanceScore() > 1
                || !seenIndexes.add(result.index())) {
            throw invalidResponse("invalid index or relevance score");
        }
    }

    private String documentText(SemanticSearchHit hit) { // 方法作用：完成“documentText”对应的内部步骤，供主流程复用。
        return (hit.headingPath() == null ? "" : hit.headingPath()) + "\n\n" + hit.childText();
    }

    private SemanticSearchHit withScore(SemanticSearchHit hit, double score) { // 方法作用：完成“withScore”对应的内部步骤，供主流程复用。
        return new SemanticSearchHit(
                hit.chunkId(), hit.parentId(), hit.documentId(), hit.filename(), hit.headingPath(),
                hit.childText(), hit.parentText(), score);
    }

    private void validateConfiguration() { // 方法作用：在发起远程调用前检查地址、密钥等必填配置。
        if (properties.endpoint() == null || properties.endpoint().isBlank()
                || properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new DocumentException(
                    "RERANKING_CONFIGURATION_MISSING",
                    "Qwen reranking requires QWEN_RERANK_URL and DASHSCOPE_API_KEY",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private DocumentException invalidResponse(String reason) { // 方法作用：完成“invalidResponse”对应的内部步骤，供主流程复用。
        return new DocumentException(
                "RERANKING_PROVIDER_INVALID_RESPONSE",
                "Qwen reranking returned an invalid response: " + reason,
                HttpStatus.BAD_GATEWAY);
    }

    record QwenRerankRequest(String model /* 字段作用：实际使用的模型名称，用于追踪和审计。 */, QwenRerankInput input /* 字段作用：该不可变对象携带的 input 数据。 */, QwenRerankParameters parameters /* 字段作用：该不可变对象携带的 parameters 数据。 */) { // 类型作用：请求 DTO，规定调用接口时可以提交哪些 JSON 字段。
    }

    record QwenRerankInput(String query /* 字段作用：用户输入的问题或检索文本。 */, List<String> documents /* 字段作用：该不可变对象携带的 documents 数据。 */) { // 类型作用：Qwen 适配器，负责请求组装、远程调用和响应校验。
    }

    record QwenRerankParameters(
            @JsonProperty("top_n") int topN /* 字段作用：该不可变对象携带的 topN 数据。 */,
            String instruct /* 字段作用：该不可变对象携带的 instruct 数据。 */) { // 类型作用：Qwen 适配器，负责请求组装、远程调用和响应校验。
    }

    record QwenRerankResponse(QwenRerankOutput output /* 字段作用：该不可变对象携带的 output 数据。 */, QwenRerankUsage usage /* 字段作用：该不可变对象携带的 usage 数据。 */,
                              @JsonProperty("request_id") String requestId /* 字段作用：对应对象的唯一标识。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。
    }

    record QwenRerankOutput(List<QwenRerankResult> results /* 字段作用：该不可变对象携带的 results 数据。 */) { // 类型作用：Qwen 适配器，负责请求组装、远程调用和响应校验。
    }

    record QwenRerankResult(int index /* 字段作用：该不可变对象携带的 index 数据。 */, @JsonProperty("relevance_score") double relevanceScore /* 字段作用：相关性或质量分数，含义由当前算法定义。 */) { // 类型作用：Qwen 适配器，负责请求组装、远程调用和响应校验。
    }

    record QwenRerankUsage(
            @JsonProperty("prompt_tokens") Integer promptTokens /* 字段作用：模型输入或输出的 Token 数量。 */,
            @JsonProperty("total_tokens") Integer totalTokens /* 字段作用：模型输入或输出的 Token 数量。 */) { // 类型作用：Qwen 适配器，负责请求组装、远程调用和响应校验。
    }
}
