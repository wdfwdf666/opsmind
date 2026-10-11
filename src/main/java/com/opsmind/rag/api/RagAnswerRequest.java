package com.opsmind.rag.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record RagAnswerRequest(
        @Schema(example = "数据库连接池告警应该如何处置？") String query,
        @Schema(example = "5", nullable = true, description = "Number of reranked evidence chunks") Integer topK,
        @Schema(format = "uuid", nullable = true, description = "Optional document scope") UUID documentId) {
}
