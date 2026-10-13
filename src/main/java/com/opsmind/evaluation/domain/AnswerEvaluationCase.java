package com.opsmind.evaluation.domain;

import java.util.List;

public record AnswerEvaluationCase(
        String id,
        String query,
        boolean answerable,
        List<List<String>> expectedFactGroups,
        List<String> expectedHeadingPaths) {
}
