package com.opsmind.evaluation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.evaluation.domain.EvaluationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerEvaluationDatasetLoaderTests {

    private final AnswerEvaluationDatasetLoader loader =
            new AnswerEvaluationDatasetLoader(new ObjectMapper());

    @Test
    void shouldLoadVersionedAnswerDataset() {
        var dataset = loader.load(null);
        var historicalDataset = loader.load("opsmind-answer-v1");

        assertThat(dataset.name()).isEqualTo("opsmind-answer-v2");
        assertThat(dataset.topK()).isEqualTo(3);
        assertThat(dataset.cases()).hasSize(20);
        assertThat(dataset.cases()).filteredOn(evaluationCase -> evaluationCase.answerable()).hasSize(6);
        assertThat(dataset.cases()).extracting(evaluationCase -> evaluationCase.id()).doesNotHaveDuplicates();
        assertThat(historicalDataset.name()).isEqualTo("opsmind-answer-v1");
        assertThat(historicalDataset.cases()).hasSize(8);
    }

    @Test
    void shouldRejectUnknownDatasetWithoutResolvingUserPath() {
        assertThatThrownBy(() -> loader.load("../../application"))
                .isInstanceOf(EvaluationException.class)
                .satisfies(exception -> assertThat(((EvaluationException) exception).code())
                        .isEqualTo("EVALUATION_DATASET_NOT_FOUND"));
    }
}
