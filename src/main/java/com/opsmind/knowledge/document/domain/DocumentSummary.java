package com.opsmind.knowledge.document.domain;

import java.util.UUID;

public record DocumentSummary(
        KnowledgeDocument document,
        UUID taskId,
        IngestionTaskStatus taskStatus,
        String taskError) {
}
