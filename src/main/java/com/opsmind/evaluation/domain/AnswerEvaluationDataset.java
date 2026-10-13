package com.opsmind.evaluation.domain;

import java.util.List;

public record AnswerEvaluationDataset(
        String name,
        String description,
        int topK,
        List<AnswerEvaluationCase> cases) {
}
