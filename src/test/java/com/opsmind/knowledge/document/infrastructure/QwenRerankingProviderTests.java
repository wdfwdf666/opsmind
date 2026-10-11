package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.config.RerankingProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenRerankingProviderTests {

    @Test
    void shouldCallQwenApiAndMapOriginalCandidateIndexes() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenRerankingProvider provider = new QwenRerankingProvider(properties("https://workspace.example/rerank", "secret"), builder);
        SemanticSearchHit first = hit("First");
        SemanticSearchHit second = hit("Second");
        server.expect(requestTo("https://workspace.example/rerank"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret"))
                .andExpect(content().json("""
                        {"model":"qwen3.7-text-rerank",
                         "input":{"query":"which one","documents":["First\n\nchild First","Second\n\nchild Second"]},
                         "parameters":{"top_n":2,"instruct":"Retrieve answers."}}
                        """))
                .andRespond(withSuccess("""
                        {"output":{"results":[
                          {"index":1,"relevance_score":0.91},
                          {"index":0,"relevance_score":0.24}]},
                         "usage":{"prompt_tokens":12,"total_tokens":12},"request_id":"request-1"}
                        """, MediaType.APPLICATION_JSON));

        List<SemanticSearchHit> result = provider.rerank("which one", List.of(first, second), 2);

        assertThat(result).extracting(SemanticSearchHit::chunkId)
                .containsExactly(second.chunkId(), first.chunkId());
        assertThat(result).extracting(SemanticSearchHit::score).containsExactly(0.91, 0.24);
        server.verify();
    }

    @Test
    void shouldRejectMissingConfigurationBeforeSendingRequest() {
        QwenRerankingProvider provider = new QwenRerankingProvider(properties("", ""), RestClient.builder());

        assertThatThrownBy(() -> provider.rerank("query", List.of(hit("candidate")), 1))
                .isInstanceOf(DocumentException.class)
                .satisfies(exception -> assertThat(((DocumentException) exception).code())
                        .isEqualTo("RERANKING_CONFIGURATION_MISSING"));
    }

    @Test
    void shouldMapRemoteFailureWithoutLeakingResponseBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenRerankingProvider provider = new QwenRerankingProvider(properties("https://workspace.example/rerank", "secret"), builder);
        server.expect(requestTo("https://workspace.example/rerank"))
                .andRespond(withServerError().body("sensitive upstream details"));

        assertThatThrownBy(() -> provider.rerank("query", List.of(hit("candidate")), 1))
                .isInstanceOf(DocumentException.class)
                .satisfies(exception -> {
                    DocumentException documentException = (DocumentException) exception;
                    assertThat(documentException.code()).isEqualTo("RERANKING_PROVIDER_ERROR");
                    assertThat(documentException.getMessage()).doesNotContain("sensitive");
                });
        server.verify();
    }

    private RerankingProperties properties(String endpoint, String apiKey) {
        return new RerankingProperties(
                "qwen", "qwen3.7-text-rerank", endpoint, apiKey, "Retrieve answers.", 4, 20);
    }

    private SemanticSearchHit hit(String heading) {
        return new SemanticSearchHit(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "runbook.md", heading,
                "child " + heading, "parent " + heading, 0.03);
    }
}
