package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.IngestionTask;
import com.opsmind.knowledge.document.domain.IngestionTaskRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryIngestionTaskRepository implements IngestionTaskRepository {

    private final ConcurrentMap<UUID, IngestionTask> tasksByDocumentId = new ConcurrentHashMap<>();

    @Override
    public IngestionTask getOrCreate(UUID documentId, Instant now) {
        return tasksByDocumentId.computeIfAbsent(documentId, id -> IngestionTask.pending(id, now));
    }

    @Override
    public Optional<IngestionTask> findByDocumentId(UUID documentId) {
        return Optional.ofNullable(tasksByDocumentId.get(documentId));
    }
}

