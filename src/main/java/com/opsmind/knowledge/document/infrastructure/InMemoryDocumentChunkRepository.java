package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.DocumentChunk;
import com.opsmind.knowledge.document.domain.DocumentChunkPage;
import com.opsmind.knowledge.document.domain.DocumentChunkRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class InMemoryDocumentChunkRepository implements DocumentChunkRepository { // 类型作用：内存版实现，主要供单元测试使用，重启后数据消失。

    private final List<DocumentChunk> chunks = new ArrayList<>();

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public synchronized void replaceAll(UUID documentId, List<DocumentChunk> replacements) { // 方法作用：更新数据或状态，并保持并发一致性。
        chunks.removeIf(chunk -> chunk.documentId().equals(documentId));
        chunks.addAll(replacements);
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public synchronized DocumentChunkPage findPage(UUID documentId, ChunkType type, int page, int size) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        List<DocumentChunk> matching = findAll(documentId, type);
        int fromIndex = (int) Math.min((long) page * size, matching.size());
        int toIndex = Math.min(fromIndex + size, matching.size());
        return new DocumentChunkPage(
                List.copyOf(matching.subList(fromIndex, toIndex)), type, page, size, matching.size());
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public synchronized List<DocumentChunk> findAll(UUID documentId, ChunkType type) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return chunks.stream()
                .filter(chunk -> chunk.documentId().equals(documentId) && chunk.type() == type)
                .sorted(Comparator.comparingInt(DocumentChunk::ordinal))
                .toList();
    }
}
