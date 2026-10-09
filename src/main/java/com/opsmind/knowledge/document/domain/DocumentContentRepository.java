package com.opsmind.knowledge.document.domain;

import java.util.Optional;
import java.util.UUID;

public interface DocumentContentRepository {

    void save(DocumentContent content);

    Optional<DocumentContent> findByDocumentId(UUID documentId);
}
