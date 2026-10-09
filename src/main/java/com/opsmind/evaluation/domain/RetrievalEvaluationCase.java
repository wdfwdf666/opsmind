package com.opsmind.evaluation.domain;

import java.util.List;

public record RetrievalEvaluationCase(
        String id,
        String query,
        List<String> expectedHeadingPaths) {
}
