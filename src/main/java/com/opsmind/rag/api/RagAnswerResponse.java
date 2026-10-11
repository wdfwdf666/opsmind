package com.opsmind.rag.api;

import com.opsmind.rag.application.RagAnswerResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record RagAnswerResponse(
        String query,
        @Schema(format = "uuid", nullable = true) UUID documentId,
        String retrievalModel,
        String generationModel,
        boolean refused,
        String answer,
        List<RagCitationResponse> citations,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens) {

    static RagAnswerResponse from(RagAnswerResult result) {
        return new RagAnswerResponse(
                result.query(), result.documentId(), result.retrievalModel(), result.generationModel(),
                result.refused(), result.answer(),
                result.citations().stream().map(RagCitationResponse::from).toList(),
                result.promptTokens(), result.completionTokens(), result.totalTokens());
    }
}
