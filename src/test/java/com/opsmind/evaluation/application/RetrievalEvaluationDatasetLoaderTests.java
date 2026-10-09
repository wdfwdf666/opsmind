package com.opsmind.evaluation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.evaluation.domain.EvaluationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetrievalEvaluationDatasetLoaderTests {

    private final RetrievalEvaluationDatasetLoader loader =
            new RetrievalEvaluationDatasetLoader(new ObjectMapper());

    @Test
    void shouldLoadVersionedDatasetWithStableCutoffs() {
        var dataset = loader.load("opsmind-retrieval-v1");

        assertThat(dataset.name()).isEqualTo("opsmind-retrieval-v1");
        assertThat(dataset.cutoffs()).containsExactly(1, 3);
        assertThat(dataset.cases()).hasSize(6);
        assertThat(dataset.cases()).extracting(evaluationCase -> evaluationCase.id()).doesNotHaveDuplicates();
    }

    @Test
    void shouldRejectUnknownDatasetWithoutBuildingArbitraryResourcePath() {
        assertThatThrownBy(() -> loader.load("../../application"))
                .isInstanceOf(EvaluationException.class)
                .satisfies(exception -> assertThat(((EvaluationException) exception).code())
                        .isEqualTo("EVALUATION_DATASET_NOT_FOUND"));
    }
}
