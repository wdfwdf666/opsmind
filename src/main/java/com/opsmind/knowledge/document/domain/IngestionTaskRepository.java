package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IngestionTaskRepository { // 类型作用：仓储接口，业务层依赖它而不是依赖具体 SQL。

    IngestionTask getOrCreate(UUID documentId, Instant now); // 接口约定：规定实现类必须按条件读取目标数据，不存在时按约定返回空值或异常。

    Optional<IngestionTask> findByDocumentId(UUID documentId); // 接口约定：规定实现类必须按条件读取目标数据，不存在时按约定返回空值或异常。

    boolean tryMarkRunning(UUID documentId, Instant now, Instant staleBefore); // 接口约定：规定实现类必须完成“tryMarkRunning”对应的内部步骤，供主流程复用。

    void markSucceeded(UUID documentId, Instant now); // 接口约定：规定实现类必须更新数据或状态，并保持并发一致性。

    void markFailed(UUID documentId, Instant now, String errorMessage); // 接口约定：规定实现类必须更新数据或状态，并保持并发一致性。
}

