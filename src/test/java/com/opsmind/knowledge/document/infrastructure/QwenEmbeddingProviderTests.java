package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.config.EmbeddingProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenEmbeddingProviderTests {

    @Test
    void shouldCallOpenAiCompatibleApiInBatchesAndRestoreIndexOrder() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        EmbeddingProperties properties = properties("https://workspace.example/compatible-mode/v1/", "secret", 2);
        QwenEmbeddingProvider provider = new QwenEmbeddingProvider(properties, builder);
        server.expect(requestTo("https://workspace.example/compatible-mode/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret"))
                .andExpect(content().json("""
                        {"model":"qwen3.7-text-embedding","input":["first","second"],
                         "dimensions":8,"encoding_format":"float"}
                        """))
                .andRespond(withSuccess("""
                        {"data":[
                          {"index":1,"embedding":[0,1,0,0,0,0,0,0]},
                          {"index":0,"embedding":[1,0,0,0,0,0,0,0]}],
                         "model":"qwen3.7-text-embedding",
                         "usage":{"prompt_tokens":4,"total_tokens":4}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://workspace.example/compatible-mode/v1/embeddings"))
                .andExpect(content().json("""
                        {"model":"qwen3.7-text-embedding","input":["third"],
                         "dimensions":8,"encoding_format":"float"}
                        """))
                .andRespond(withSuccess("""
                        {"data":[{"index":0,"embedding":[0,0,1,0,0,0,0,0]}],
                         "model":"qwen3.7-text-embedding"}
                        """, MediaType.APPLICATION_JSON));

        List<double[]> vectors = provider.embed(List.of("first", "second", "third"));

        assertThat(vectors).hasSize(3);
        assertThat(vectors.get(0)).containsExactly(1, 0, 0, 0, 0, 0, 0, 0);
        assertThat(vectors.get(1)).containsExactly(0, 1, 0, 0, 0, 0, 0, 0);
        assertThat(vectors.get(2)).containsExactly(0, 0, 1, 0, 0, 0, 0, 0);
        server.verify();
    }

    @Test
    void shouldRejectMissingCredentialsWithoutSendingARequest() {
        QwenEmbeddingProvider provider = new QwenEmbeddingProvider(
                properties("", "", 20), RestClient.builder());

        assertThatThrownBy(() -> provider.embed(List.of("query")))
                .isInstanceOf(DocumentException.class)
                .satisfies(exception -> assertThat(((DocumentException) exception).code())
                        .isEqualTo("EMBEDDING_CONFIGURATION_MISSING"));
    }

    @Test
    void shouldMapRemoteFailureWithoutLeakingResponseBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenEmbeddingProvider provider = new QwenEmbeddingProvider(
                properties("https://workspace.example/compatible-mode/v1", "secret", 20), builder);
        server.expect(requestTo("https://workspace.example/compatible-mode/v1/embeddings"))
                .andRespond(withServerError().body("sensitive upstream details"));

        assertThatThrownBy(() -> provider.embed(List.of("query")))
                .isInstanceOf(DocumentException.class)
                .satisfies(exception -> {
                    DocumentException documentException = (DocumentException) exception;
                    assertThat(documentException.code()).isEqualTo("EMBEDDING_PROVIDER_ERROR");
                    assertThat(documentException.getMessage()).doesNotContain("sensitive");
                });
        server.verify();
    }

    private EmbeddingProperties properties(String baseUrl, String apiKey, int batchSize) {
        return new EmbeddingProperties(
                "qwen", "qwen3.7-text-embedding", 8, 5, 50, baseUrl, apiKey, batchSize);
    }
}
