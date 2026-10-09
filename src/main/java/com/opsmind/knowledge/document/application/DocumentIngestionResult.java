package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.DocumentContent;
import com.opsmind.knowledge.document.domain.IngestionTask;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;

public record DocumentIngestionResult(
        KnowledgeDocument document,
        IngestionTask task,
        DocumentContent content) {
}
