package com.opsmind.evaluation.application;

import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.evaluation.domain.EvaluationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRoutingEvaluationDatasetLoaderTests {

    private final AgentRoutingEvaluationDatasetLoader loader = new AgentRoutingEvaluationDatasetLoader();

    @Test
    void shouldLoadBalancedVersionedRoutingDataset() {
        var dataset = loader.load(null);

        assertThat(dataset.name()).isEqualTo("opsmind-agent-routing-v1");
        assertThat(dataset.cases()).hasSize(24);
        assertThat(dataset.cases()).filteredOn(item -> item.expectedTaskType() == AgentTaskType.KNOWLEDGE_QA)
                .hasSize(12);
        assertThat(dataset.cases()).filteredOn(item -> item.expectedTaskType() == AgentTaskType.CREATE_TICKET)
                .hasSize(12);
        assertThat(dataset.cases()).extracting("category")
                .contains("NEGATION", "PROMPT_INJECTION", "INDIRECT_WRITE");
    }

    @Test
    void shouldRejectArbitraryDatasetPath() {
        assertThatThrownBy(() -> loader.load("../../application"))
                .isInstanceOf(EvaluationException.class)
                .satisfies(exception -> assertThat(((EvaluationException) exception).code())
                        .isEqualTo("EVALUATION_DATASET_NOT_FOUND"));
    }
}
