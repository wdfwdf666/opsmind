package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.DocumentChunk;
import com.opsmind.knowledge.document.domain.DocumentChunkPage;
import com.opsmind.knowledge.document.domain.DocumentChunkRepository;
import com.opsmind.knowledge.document.domain.DocumentContent;
import com.opsmind.knowledge.document.domain.DocumentContentRepository;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * 文档分块用例：读取已解析正文，调用纯分块算法，并原子替换数据库中的旧分块。
 */
@Service
public class DocumentChunkingService {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 100;

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository contentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final HeadingAwareDocumentChunker chunker;
    private final Clock clock;

    @Autowired
    public DocumentChunkingService(
            DocumentRepository documentRepository,
            DocumentContentRepository contentRepository,
            DocumentChunkRepository chunkRepository,
            HeadingAwareDocumentChunker chunker) {
        this(documentRepository, contentRepository, chunkRepository, chunker, Clock.systemUTC());
    }

    DocumentChunkingService(
            DocumentRepository documentRepository,
            DocumentContentRepository contentRepository,
            DocumentChunkRepository chunkRepository,
            HeadingAwareDocumentChunker chunker,
            Clock clock) {
        this.documentRepository = documentRepository;
        this.contentRepository = contentRepository;
        this.chunkRepository = chunkRepository;
        this.chunker = chunker;
        this.clock = clock;
    }

    public DocumentChunkingResult rebuild(UUID documentId) {
        requireDocument(documentId);
        // 解析完成是分块的前置条件，因此缺少正文返回 409 Conflict，而不是 404。
        DocumentContent content = contentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentException(
                        "DOCUMENT_CONTENT_NOT_READY",
                        "Document content must be parsed before chunking",
                        HttpStatus.CONFLICT));
        List<DocumentChunk> chunks = chunker.chunk(
                documentId, content.title(), content.text(), clock.instant());
        // replaceAll 应在事务中先删后插；任何一步失败都回滚，防止只留下半套分块。
        chunkRepository.replaceAll(documentId, chunks);
        int parentCount = (int) chunks.stream().filter(chunk -> chunk.type() == ChunkType.PARENT).count();
        int childCount = chunks.size() - parentCount;
        return new DocumentChunkingResult(documentId, parentCount, childCount);
    }

    public DocumentChunkPage list(UUID documentId, ChunkType type, int page, int size) {
        requireDocument(documentId);
        if (page < 0) {
            throw new DocumentException(
                    "DOCUMENT_CHUNK_PAGE_INVALID", "Page must be greater than or equal to 0", HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DocumentException(
                    "DOCUMENT_CHUNK_PAGE_SIZE_INVALID",
                    "Page size must be between 1 and " + MAX_PAGE_SIZE,
                    HttpStatus.BAD_REQUEST);
        }
        return chunkRepository.findPage(documentId, type, page, size);
    }

    private void requireDocument(UUID documentId) {
        if (documentRepository.findById(documentId).isEmpty()) {
            throw new DocumentException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND);
        }
    }
}
