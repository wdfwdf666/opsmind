package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository { // 类型作用：仓储接口，业务层依赖它而不是依赖具体 SQL。

    Optional<KnowledgeDocument> findById(UUID id); // 接口约定：规定实现类必须按条件读取目标数据，不存在时按约定返回空值或异常。

    Optional<KnowledgeDocument> findBySha256(String sha256); // 接口约定：规定实现类必须按条件读取目标数据，不存在时按约定返回空值或异常。

    SaveResult saveIfAbsent(KnowledgeDocument document); // 接口约定：规定实现类必须创建或保存数据，并返回持久化结果。

    void updateStatus(UUID documentId, DocumentStatus status); // 接口约定：规定实现类必须更新数据或状态，并保持并发一致性。

    record SaveResult(KnowledgeDocument document /* 字段作用：该不可变对象携带的 document 数据。 */, boolean created /* 字段作用：该不可变对象携带的 created 数据。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
    }
}

