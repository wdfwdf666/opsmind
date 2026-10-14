package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.List;
import java.util.UUID;

public interface ChunkEmbeddingRepository { // 类型作用：仓储接口，业务层依赖它而不是依赖具体 SQL。

    void replaceAll(UUID documentId, String model, List<ChunkEmbedding> embeddings); // 接口约定：规定实现类必须更新数据或状态，并保持并发一致性。

    NearestChunkSearch findNearest(String model, double[] queryVector, UUID documentId, int limit); // 接口约定：规定实现类必须按条件读取目标数据，不存在时按约定返回空值或异常。
}
