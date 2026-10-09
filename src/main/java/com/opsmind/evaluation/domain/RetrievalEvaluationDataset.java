package com.opsmind.evaluation.domain;

import java.util.List;

public record RetrievalEvaluationDataset(
        String name,
        String description,
        List<Integer> cutoffs,
        List<RetrievalEvaluationCase> cases) {
}
