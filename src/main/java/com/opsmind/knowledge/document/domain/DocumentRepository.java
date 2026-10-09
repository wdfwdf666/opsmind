package com.opsmind.knowledge.document.domain;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {

    Optional<KnowledgeDocument> findById(UUID id);

    Optional<KnowledgeDocument> findBySha256(String sha256);

    SaveResult saveIfAbsent(KnowledgeDocument document);

    record SaveResult(KnowledgeDocument document, boolean created) {
    }
}

