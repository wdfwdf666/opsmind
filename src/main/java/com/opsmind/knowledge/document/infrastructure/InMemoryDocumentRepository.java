package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.DocumentRepository;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryDocumentRepository implements DocumentRepository {

    private final ConcurrentMap<UUID, KnowledgeDocument> documentsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, KnowledgeDocument> documentsByChecksum = new ConcurrentHashMap<>();

    @Override
    public Optional<KnowledgeDocument> findById(UUID id) {
        return Optional.ofNullable(documentsById.get(id));
    }

    @Override
    public Optional<KnowledgeDocument> findBySha256(String sha256) {
        return Optional.ofNullable(documentsByChecksum.get(sha256));
    }

    @Override
    public synchronized SaveResult saveIfAbsent(KnowledgeDocument document) {
        KnowledgeDocument existing = documentsByChecksum.get(document.sha256());
        if (existing != null) {
            return new SaveResult(existing, false);
        }
        documentsByChecksum.put(document.sha256(), document);
        documentsById.put(document.id(), document);
        return new SaveResult(document, true);
    }
}

