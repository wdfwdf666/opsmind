package com.opsmind.evaluation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.evaluation.domain.AnswerEvaluationCase;
import com.opsmind.evaluation.domain.AnswerEvaluationDataset;
import com.opsmind.evaluation.domain.EvaluationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
public class AnswerEvaluationDatasetLoader {

    public static final String DEFAULT_DATASET = "opsmind-answer-v1";

    private static final Map<String, String> DATASETS = Map.of(
            DEFAULT_DATASET, "evaluation/opsmind-answer-v1.json");

    private final ObjectMapper objectMapper;

    public AnswerEvaluationDatasetLoader() {
        this(new ObjectMapper());
    }

    AnswerEvaluationDatasetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AnswerEvaluationDataset load(String requestedName) {
        String name = requestedName == null || requestedName.isBlank()
                ? DEFAULT_DATASET
                : requestedName.strip();
        String location = DATASETS.get(name);
        if (location == null) {
            throw new EvaluationException(
                    "EVALUATION_DATASET_NOT_FOUND", "Evaluation dataset not found: " + name, HttpStatus.NOT_FOUND);
        }
        try (var inputStream = new ClassPathResource(location).getInputStream()) {
            AnswerEvaluationDataset dataset = objectMapper.readValue(inputStream, AnswerEvaluationDataset.class);
            validate(dataset, name);
            return dataset;
        } catch (IOException exception) {
            throw new EvaluationException(
                    "EVALUATION_DATASET_INVALID", "Evaluation dataset could not be loaded", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validate(AnswerEvaluationDataset dataset, String requestedName) {
        if (dataset == null || !requestedName.equals(dataset.name())
                || dataset.description() == null || dataset.description().isBlank()
                || dataset.topK() < 1 || dataset.topK() > 50
                || dataset.cases() == null || dataset.cases().isEmpty()) {
            throw invalidDataset();
        }
        HashSet<String> ids = new HashSet<>();
        for (AnswerEvaluationCase evaluationCase : dataset.cases()) {
            if (!validCase(evaluationCase) || !ids.add(evaluationCase.id())) {
                throw invalidDataset();
            }
        }
    }

    private boolean validCase(AnswerEvaluationCase evaluationCase) {
        if (evaluationCase == null || evaluationCase.id() == null || evaluationCase.id().isBlank()
                || evaluationCase.query() == null || evaluationCase.query().isBlank()
                || evaluationCase.expectedFactGroups() == null
                || evaluationCase.expectedHeadingPaths() == null) {
            return false;
        }
        boolean factsValid = evaluationCase.expectedFactGroups().stream().allMatch(this::validFactGroup);
        boolean headingsValid = evaluationCase.expectedHeadingPaths().stream()
                .allMatch(heading -> heading != null && !heading.isBlank());
        if (!factsValid || !headingsValid) {
            return false;
        }
        return evaluationCase.answerable()
                ? !evaluationCase.expectedFactGroups().isEmpty() && !evaluationCase.expectedHeadingPaths().isEmpty()
                : evaluationCase.expectedFactGroups().isEmpty() && evaluationCase.expectedHeadingPaths().isEmpty();
    }

    private boolean validFactGroup(List<String> group) {
        return group != null && !group.isEmpty()
                && group.stream().allMatch(fact -> fact != null && !fact.isBlank());
    }

    private EvaluationException invalidDataset() {
        return new EvaluationException(
                "EVALUATION_DATASET_INVALID", "Evaluation dataset is invalid", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
