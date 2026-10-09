package com.opsmind.evaluation.domain;

public record RetrievalMetricAtK(
        int k,
        double recall,
        double meanReciprocalRank,
        double hitRate) {
}
