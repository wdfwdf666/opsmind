package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.ChunkEmbedding;
import com.opsmind.knowledge.document.domain.ChunkEmbeddingRepository;
import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.DocumentChunk;
import com.opsmind.knowledge.document.domain.DocumentChunkRepository;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentEmbeddingService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ChunkEmbeddingRepository embeddingRepository;
    private final EmbeddingProvider embeddingProvider;
    private final Clock clock;

    @Autowired
    public DocumentEmbeddingService(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            ChunkEmbeddingRepository embeddingRepository,
            EmbeddingProvider embeddingProvider) {
        this(documentRepository, chunkRepository, embeddingRepository, embeddingProvider, Clock.systemUTC());
    }

    DocumentEmbeddingService(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            ChunkEmbeddingRepository embeddingRepository,
            EmbeddingProvider embeddingProvider,
            Clock clock) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingProvider = embeddingProvider;
        this.clock = clock;
    }

    public DocumentEmbeddingResult rebuild(UUID documentId) {
        if (documentRepository.findById(documentId).isEmpty()) {
            throw new DocumentException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND);
        }
        List<DocumentChunk> chunks = chunkRepository.findAll(documentId, ChunkType.CHILD);
        if (chunks.isEmpty()) {
            throw new DocumentException(
                    "DOCUMENT_CHUNKS_NOT_READY", "Child chunks must be built before embedding", HttpStatus.CONFLICT);
        }
        List<String> inputs = chunks.stream().map(this::embeddingInput).toList();
        List<double[]> vectors = embeddingProvider.embed(inputs);
        validateVectors(chunks.size(), vectors);
        Instant createdAt = clock.instant();
        List<ChunkEmbedding> embeddings = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            DocumentChunk chunk = chunks.get(index);
            embeddings.add(new ChunkEmbedding(
                    chunk.id(), documentId, embeddingProvider.model(), embeddingProvider.dimension(),
                    vectors.get(index), chunk.contentHash(), createdAt));
        }
        embeddingRepository.replaceAll(documentId, embeddingProvider.model(), embeddings);
        return new DocumentEmbeddingResult(
                documentId, embeddingProvider.model(), embeddingProvider.dimension(), embeddings.size());
    }

    private String embeddingInput(DocumentChunk chunk) {
        return chunk.headingPath() == null || chunk.headingPath().isBlank()
                ? chunk.text()
                : chunk.headingPath() + "\n\n" + chunk.text();
    }

    private void validateVectors(int expectedSize, List<double[]> vectors) {
        if (vectors.size() != expectedSize) {
            throw new IllegalStateException("Embedding provider returned an unexpected number of vectors");
        }
        if (vectors.stream().anyMatch(vector -> vector.length != embeddingProvider.dimension())) {
            throw new IllegalStateException("Embedding provider returned an unexpected vector dimension");
        }
    }
}
