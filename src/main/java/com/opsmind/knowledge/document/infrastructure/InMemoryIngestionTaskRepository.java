package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import com.opsmind.knowledge.document.domain.IngestionTask;
import com.opsmind.knowledge.document.domain.IngestionTaskRepository;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryIngestionTaskRepository implements IngestionTaskRepository { // 类型作用：内存版实现，主要供单元测试使用，重启后数据消失。

    private final ConcurrentMap<UUID, IngestionTask> tasksByDocumentId = new ConcurrentHashMap<>();

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public IngestionTask getOrCreate(UUID documentId, Instant now) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return tasksByDocumentId.computeIfAbsent(documentId, id -> IngestionTask.pending(id, now));
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public Optional<IngestionTask> findByDocumentId(UUID documentId) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return Optional.ofNullable(tasksByDocumentId.get(documentId));
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public synchronized boolean tryMarkRunning(UUID documentId, Instant now, Instant staleBefore) { // 方法作用：完成“tryMarkRunning”对应的内部步骤，供主流程复用。
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

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public synchronized void markSucceeded(UUID documentId, Instant now) { // 方法作用：更新数据或状态，并保持并发一致性。
        update(documentId, IngestionTaskStatus.SUCCEEDED, now, null);
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public synchronized void markFailed(UUID documentId, Instant now, String errorMessage) { // 方法作用：更新数据或状态，并保持并发一致性。
        update(documentId, IngestionTaskStatus.FAILED, now, errorMessage);
    }

    private void update(UUID documentId, IngestionTaskStatus status, Instant now, String errorMessage) { // 方法作用：更新数据或状态，并保持并发一致性。
        IngestionTask current = tasksByDocumentId.get(documentId);
        if (current != null) {
            tasksByDocumentId.put(documentId, new IngestionTask(
                    current.id(), current.documentId(), status, current.createdAt(), now, errorMessage));
        }
    }
}

