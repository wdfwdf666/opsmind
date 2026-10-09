package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.IngestionTask;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;

public record DocumentUploadResult(
        KnowledgeDocument document,
        IngestionTask task,
        boolean duplicate) {
}

