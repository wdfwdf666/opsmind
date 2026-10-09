package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.DocumentContent;
import com.opsmind.knowledge.document.domain.DocumentContentRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryDocumentContentRepository implements DocumentContentRepository {

    private final ConcurrentMap<UUID, DocumentContent> contents = new ConcurrentHashMap<>();

    @Override
    public void save(DocumentContent content) {
        contents.put(content.documentId(), content);
    }

    @Override
    public Optional<DocumentContent> findByDocumentId(UUID documentId) {
        return Optional.ofNullable(contents.get(documentId));
    }
}
