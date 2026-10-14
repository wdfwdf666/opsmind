package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import com.opsmind.knowledge.document.domain.DocumentContent;
import com.opsmind.knowledge.document.domain.DocumentContentRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryDocumentContentRepository implements DocumentContentRepository { // 类型作用：内存版实现，主要供单元测试使用，重启后数据消失。

    private final ConcurrentMap<UUID, DocumentContent> contents = new ConcurrentHashMap<>();

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public void save(DocumentContent content) { // 方法作用：创建或保存数据，并返回持久化结果。
        contents.put(content.documentId(), content);
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public Optional<DocumentContent> findByDocumentId(UUID documentId) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return Optional.ofNullable(contents.get(documentId));
    }
}
