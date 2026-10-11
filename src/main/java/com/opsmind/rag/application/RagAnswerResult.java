package com.opsmind.rag.application;

import java.util.List;
import java.util.UUID;

public record RagAnswerResult(
        String query,
        UUID documentId,
        String retrievalModel,
        String generationModel,
        boolean refused,
        String answer,
        List<RagCitation> citations,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens) {
}
