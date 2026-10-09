package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.IngestionTask;
import com.opsmind.knowledge.document.domain.IngestionTaskRepository;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    @Override
    public synchronized boolean tryMarkRunning(UUID documentId, Instant now, Instant staleBefore) {
        IngestionTask current = tasksByDocumentId.get(documentId);
        boolean activeLease = current != null
                && current.status() == IngestionTaskStatus.RUNNING
                && !current.updatedAt().isBefore(staleBefore);
        if (current == null || activeLease || current.status() == IngestionTaskStatus.SUCCEEDED) {
            return false;
        }
        tasksByDocumentId.put(documentId, new IngestionTask(
                current.id(), current.documentId(), IngestionTaskStatus.RUNNING,
                current.createdAt(), now, null));
        return true;
    }

    @Override
    public synchronized void markSucceeded(UUID documentId, Instant now) {
        update(documentId, IngestionTaskStatus.SUCCEEDED, now, null);
    }

    @Override
    public synchronized void markFailed(UUID documentId, Instant now, String errorMessage) {
        update(documentId, IngestionTaskStatus.FAILED, now, errorMessage);
    }

    private void update(UUID documentId, IngestionTaskStatus status, Instant now, String errorMessage) {
        IngestionTask current = tasksByDocumentId.get(documentId);
        if (current != null) {
            tasksByDocumentId.put(documentId, new IngestionTask(
                    current.id(), current.documentId(), status, current.createdAt(), now, errorMessage));
        }
    }
}

