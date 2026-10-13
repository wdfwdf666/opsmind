package com.opsmind.evaluation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationCase;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationDataset;
import com.opsmind.evaluation.domain.EvaluationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

@Component
public class AgentRoutingEvaluationDatasetLoader {

    public static final String DEFAULT_DATASET = "opsmind-agent-routing-v1";
    private static final Map<String, String> DATASETS = Map.of(
            DEFAULT_DATASET, "evaluation/opsmind-agent-routing-v1.json");

    private final ObjectMapper objectMapper;

    public AgentRoutingEvaluationDatasetLoader() {
        this(new ObjectMapper());
    }

    AgentRoutingEvaluationDatasetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AgentRoutingEvaluationDataset load(String requestedName) {
        String name = requestedName == null || requestedName.isBlank() ? DEFAULT_DATASET : requestedName.strip();
        String location = DATASETS.get(name);
        if (location == null) {
            throw new EvaluationException(
                    "EVALUATION_DATASET_NOT_FOUND", "Evaluation dataset not found: " + name, HttpStatus.NOT_FOUND);
        }
        try (var inputStream = new ClassPathResource(location).getInputStream()) {
            AgentRoutingEvaluationDataset dataset = objectMapper.readValue(
                    inputStream, AgentRoutingEvaluationDataset.class);
            validate(dataset, name);
            return dataset;
        } catch (IOException exception) {
            throw new EvaluationException(
                    "EVALUATION_DATASET_INVALID", "Evaluation dataset could not be loaded",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validate(AgentRoutingEvaluationDataset dataset, String requestedName) {
        if (dataset == null || !requestedName.equals(dataset.name())
                || dataset.description() == null || dataset.description().isBlank()
                || dataset.cases() == null || dataset.cases().isEmpty()) {
            throw invalidDataset();
        }
        HashSet<String> ids = new HashSet<>();
        for (AgentRoutingEvaluationCase evaluationCase : dataset.cases()) {
            if (evaluationCase == null || evaluationCase.id() == null || evaluationCase.id().isBlank()
                    || !ids.add(evaluationCase.id())
                    || evaluationCase.category() == null || evaluationCase.category().isBlank()
                    || evaluationCase.query() == null || evaluationCase.query().isBlank()
                    || evaluationCase.expectedTaskType() == null) {
                throw invalidDataset();
            }
        }
    }

    private EvaluationException invalidDataset() {
        return new EvaluationException(
                "EVALUATION_DATASET_INVALID", "Evaluation dataset is invalid", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
