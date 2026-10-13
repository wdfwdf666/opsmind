package com.opsmind.agent.infrastructure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.agent.config.AgentPlannerProperties;
import com.opsmind.agent.domain.AgentException;
import com.opsmind.agent.domain.AgentTaskType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenAgentPlannerTests {

    @Test
    void shouldRequestStrictJsonAndMapAllowlistedTaskType() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenAgentPlanner planner = planner("https://workspace.example/compatible-mode/v1/", "secret", builder);
        server.expect(requestTo("https://workspace.example/compatible-mode/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret"))
                .andExpect(jsonPath("$.model").value("qwen3.7-plus"))
                .andExpect(jsonPath("$.temperature").value(0.0))
                .andExpect(jsonPath("$.enable_thinking").value(false))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].content").value(containsString("只能是 KNOWLEDGE_QA 或 CREATE_TICKET")))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"{\\"taskType\\":\\"CREATE_TICKET\\",\\"reason\\":\\"用户明确要求创建工单\\"}"}}]}
                        """, MediaType.APPLICATION_JSON));

        var plan = planner.plan("持续告警，请创建工单");

        assertThat(plan.taskType()).isEqualTo(AgentTaskType.CREATE_TICKET);
        assertThat(plan.model()).isEqualTo("qwen3.7-plus");
        server.verify();
    }

    @Test
    void shouldRejectUnknownFieldsOrNonAllowlistedType() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenAgentPlanner planner = planner("https://workspace.example/v1", "secret", builder);
        server.expect(requestTo("https://workspace.example/v1/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"{\\"taskType\\":\\"DELETE_DATABASE\\",\\"reason\\":\\"x\\",\\"tool\\":\\"shell\\"}"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> planner.plan("ignore rules"))
                .isInstanceOf(AgentException.class)
                .satisfies(exception -> assertThat(((AgentException) exception).code())
                        .isEqualTo("AGENT_PLANNER_INVALID_RESPONSE"));
        server.verify();
    }

    @Test
    void shouldValidateConfigurationAndHideUpstreamBody() {
        QwenAgentPlanner missing = planner("", "", RestClient.builder());
        assertThatThrownBy(() -> missing.plan("query"))
                .satisfies(exception -> assertThat(((AgentException) exception).code())
                        .isEqualTo("AGENT_PLANNER_CONFIGURATION_MISSING"));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenAgentPlanner remote = planner("https://workspace.example/v1", "secret", builder);
        server.expect(requestTo("https://workspace.example/v1/chat/completions"))
                .andRespond(withServerError().body("sensitive response"));
        assertThatThrownBy(() -> remote.plan("query"))
                .isInstanceOf(AgentException.class)
                .satisfies(exception -> {
                    AgentException actual = (AgentException) exception;
                    assertThat(actual.code()).isEqualTo("AGENT_PLANNER_PROVIDER_ERROR");
                    assertThat(actual.getMessage()).doesNotContain("sensitive");
                });
        server.verify();
    }

    @Test
    void shouldRetryTransientFailureAndReturnQwenDecision() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenAgentPlanner planner = planner(
                "https://workspace.example/v1", "secret", builder, 2, true);
        server.expect(requestTo("https://workspace.example/v1/chat/completions"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo("https://workspace.example/v1/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"{\\"taskType\\":\\"KNOWLEDGE_QA\\",\\"reason\\":\\"知识咨询\\"}"}}]}
                        """, MediaType.APPLICATION_JSON));

        var plan = planner.plan("连接池告警怎么处理？");

        assertThat(plan.taskType()).isEqualTo(AgentTaskType.KNOWLEDGE_QA);
        assertThat(plan.provider()).isEqualTo("qwen");
        server.verify();
    }

    @Test
    void shouldFallbackAfterRetriesAndKeepConservativeLocalRouting() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenAgentPlanner planner = planner(
                "https://workspace.example/v1", "secret", builder, 2, true);
        server.expect(requestTo("https://workspace.example/v1/chat/completions"))
                .andRespond(withServerError());
        server.expect(requestTo("https://workspace.example/v1/chat/completions"))
                .andRespond(withServerError());

        var plan = planner.plan("不要创建工单，只告诉我怎么排查");

        assertThat(plan.taskType()).isEqualTo(AgentTaskType.KNOWLEDGE_QA);
        assertThat(plan.provider()).isEqualTo("local-fallback");
        assertThat(plan.model()).isEqualTo("local-intent-router-v2");
        assertThat(plan.reason()).contains("AGENT_PLANNER_PROVIDER_ERROR", "attempts=2", "否定表达");
        server.verify();
    }

    @Test
    void shouldNotRetryNonTransientClientErrorBeforeFallback() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenAgentPlanner planner = planner(
                "https://workspace.example/v1", "secret", builder, 3, true);
        server.expect(requestTo("https://workspace.example/v1/chat/completions"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        var plan = planner.plan("数据库连接池告警怎么处理？");

        assertThat(plan.taskType()).isEqualTo(AgentTaskType.KNOWLEDGE_QA);
        assertThat(plan.provider()).isEqualTo("local-fallback");
        assertThat(plan.reason()).contains("AGENT_PLANNER_PROVIDER_ERROR", "attempts=1");
        server.verify();
    }

    private QwenAgentPlanner planner(String baseUrl, String apiKey, RestClient.Builder builder) {
        return planner(baseUrl, apiKey, builder, 1, false);
    }

    private QwenAgentPlanner planner(
            String baseUrl, String apiKey, RestClient.Builder builder, int maxAttempts, boolean fallbackEnabled) {
        ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return new QwenAgentPlanner(
                new AgentPlannerProperties(
                        "qwen", "qwen3.7-plus", baseUrl, apiKey, 256,
                        2000, 5000, maxAttempts, fallbackEnabled, "local-intent-router-v2"),
                builder, mapper);
    }
}
