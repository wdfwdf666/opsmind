package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import com.opsmind.knowledge.document.domain.DocumentRepository;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryDocumentRepository implements DocumentRepository { // 类型作用：内存版实现，主要供单元测试使用，重启后数据消失。

    private final ConcurrentMap<UUID, KnowledgeDocument> documentsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, KnowledgeDocument> documentsByChecksum = new ConcurrentHashMap<>();

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public Optional<KnowledgeDocument> findById(UUID id) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return Optional.ofNullable(documentsById.get(id));
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public Optional<KnowledgeDocument> findBySha256(String sha256) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return Optional.ofNullable(documentsByChecksum.get(sha256));
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public synchronized SaveResult saveIfAbsent(KnowledgeDocument document) { // 方法作用：创建或保存数据，并返回持久化结果。
        KnowledgeDocument existing = documentsByChecksum.get(document.sha256());
        if (existing != null) {
            return new SaveResult(existing, false);
        }
        documentsByChecksum.put(document.sha256(), document);
        documentsById.put(document.id(), document);
        return new SaveResult(document, true);
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public synchronized void updateStatus(UUID documentId, com.opsmind.knowledge.document.domain.DocumentStatus status) { // 方法作用：更新数据或状态，并保持并发一致性。
        KnowledgeDocument current = documentsById.get(documentId);
        if (current == null) {
            return;
        }
        KnowledgeDocument updated = new KnowledgeDocument(
                current.id(), current.originalFilename(), current.mediaType(), current.size(), current.sha256(),
                current.storageKey(), status, current.createdAt());
        documentsById.put(documentId, updated);
        documentsByChecksum.put(updated.sha256(), updated);
    }
}

