package com.opsmind.evaluation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.evaluation.domain.EvaluationException;
import com.opsmind.evaluation.domain.RetrievalEvaluationCase;
import com.opsmind.evaluation.domain.RetrievalEvaluationDataset;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

@Component
public class RetrievalEvaluationDatasetLoader {

    public static final String DEFAULT_DATASET = "opsmind-retrieval-v1";

    private static final Map<String, String> DATASETS = Map.of(
            DEFAULT_DATASET, "evaluation/opsmind-retrieval-v1.json");

    private final ObjectMapper objectMapper;

    public RetrievalEvaluationDatasetLoader() {
        this(new ObjectMapper());
    }

    RetrievalEvaluationDatasetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RetrievalEvaluationDataset load(String requestedName) {
        String name = requestedName == null || requestedName.isBlank()
                ? DEFAULT_DATASET
                : requestedName.strip();
        String location = DATASETS.get(name);
        if (location == null) {
            throw new EvaluationException(
                    "EVALUATION_DATASET_NOT_FOUND", "Evaluation dataset not found: " + name, HttpStatus.NOT_FOUND);
        }
        try (var inputStream = new ClassPathResource(location).getInputStream()) {
            RetrievalEvaluationDataset dataset = objectMapper.readValue(inputStream, RetrievalEvaluationDataset.class);
            validate(dataset, name);
            return dataset;
        } catch (IOException exception) {
            throw new EvaluationException(
                    "EVALUATION_DATASET_INVALID", "Evaluation dataset could not be loaded", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validate(RetrievalEvaluationDataset dataset, String requestedName) {
        if (dataset == null || !requestedName.equals(dataset.name())
                || dataset.cases() == null || dataset.cases().isEmpty()
                || dataset.cutoffs() == null || dataset.cutoffs().isEmpty()
                || dataset.cutoffs().stream().anyMatch(k -> k == null || k < 1 || k > 50)
                || dataset.cutoffs().stream().distinct().count() != dataset.cutoffs().size()) {
            throw invalidDataset();
        }
        HashSet<String> ids = new HashSet<>();
        for (RetrievalEvaluationCase evaluationCase : dataset.cases()) {
            if (evaluationCase == null || evaluationCase.id() == null || evaluationCase.id().isBlank()
                    || !ids.add(evaluationCase.id())
                    || evaluationCase.query() == null || evaluationCase.query().isBlank()
                    || evaluationCase.expectedHeadingPaths() == null
                    || evaluationCase.expectedHeadingPaths().isEmpty()
                    || evaluationCase.expectedHeadingPaths().stream().anyMatch(
                            heading -> heading == null || heading.isBlank())) {
                throw invalidDataset();
            }
        }
    }

    private EvaluationException invalidDataset() {
        return new EvaluationException(
                "EVALUATION_DATASET_INVALID", "Evaluation dataset is invalid", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
