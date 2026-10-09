package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.DocumentContent;
import com.opsmind.knowledge.document.domain.DocumentContentRepository;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.DocumentRepository;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.IngestionTask;
import com.opsmind.knowledge.document.domain.IngestionTaskRepository;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;
    private static final Duration PROCESSING_LEASE = Duration.ofMinutes(5);

    private final DocumentStorage storage;
    private final DocumentParser parser;
    private final DocumentTextCleaner cleaner;
    private final DocumentRepository documentRepository;
    private final IngestionTaskRepository taskRepository;
    private final DocumentContentRepository contentRepository;
    private final Clock clock;

    @Autowired
    public DocumentIngestionService(
            DocumentStorage storage,
            DocumentParser parser,
            DocumentTextCleaner cleaner,
            DocumentRepository documentRepository,
            IngestionTaskRepository taskRepository,
            DocumentContentRepository contentRepository) {
        this(storage, parser, cleaner, documentRepository, taskRepository, contentRepository, Clock.systemUTC());
    }

    DocumentIngestionService(
            DocumentStorage storage,
            DocumentParser parser,
            DocumentTextCleaner cleaner,
            DocumentRepository documentRepository,
            IngestionTaskRepository taskRepository,
            DocumentContentRepository contentRepository,
            Clock clock) {
        this.storage = storage;
        this.parser = parser;
        this.cleaner = cleaner;
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.contentRepository = contentRepository;
        this.clock = clock;
    }

    public DocumentIngestionResult process(UUID documentId) {
        KnowledgeDocument document = requireDocument(documentId);
        IngestionTask task = taskRepository.getOrCreate(documentId, clock.instant());
        if (task.status() == IngestionTaskStatus.SUCCEEDED) {
            return completedResult(documentId);
        }
        Instant startedAt = clock.instant();
        if (!taskRepository.tryMarkRunning(documentId, startedAt, startedAt.minus(PROCESSING_LEASE))) {
            throw new DocumentException(
                    "DOCUMENT_PROCESSING_IN_PROGRESS",
                    "Document is already being processed",
                    HttpStatus.CONFLICT);
        }

        documentRepository.updateStatus(documentId, DocumentStatus.PARSING);
        try {
            ParsedDocument parsed = parse(document);
            String cleanedText = cleaner.clean(parsed.text());
            if (cleanedText.isBlank()) {
                throw new DocumentException(
                        "DOCUMENT_TEXT_EMPTY",
                        "No readable text was extracted from the document",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            Instant completedAt = clock.instant();
            contentRepository.save(new DocumentContent(
                    documentId,
                    normalizeTitle(parsed.title()),
                    parsed.detectedMediaType(),
                    cleanedText,
                    cleanedText.length(),
                    completedAt));
            documentRepository.updateStatus(documentId, DocumentStatus.READY);
            taskRepository.markSucceeded(documentId, completedAt);
            return completedResult(documentId);
        } catch (RuntimeException exception) {
            markFailed(documentId, exception);
            if (exception instanceof DocumentException documentException) {
                throw documentException;
            }
            throw new DocumentException(
                    "DOCUMENT_PROCESSING_FAILED",
                    "Document processing failed",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public DocumentContent findContent(UUID documentId) {
        requireDocument(documentId);
        return contentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentException(
                        "DOCUMENT_CONTENT_NOT_READY",
                        "Document content is not ready",
                        HttpStatus.CONFLICT));
    }

    private DocumentIngestionResult completedResult(UUID documentId) {
        KnowledgeDocument document = requireDocument(documentId);
        IngestionTask task = taskRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalStateException("Document task is missing"));
        DocumentContent content = contentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentException(
                        "DOCUMENT_CONTENT_NOT_READY",
                        "Document task succeeded without parsed content",
                        HttpStatus.CONFLICT));
        return new DocumentIngestionResult(document, task, content);
    }

    private KnowledgeDocument requireDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(
                        "DOCUMENT_NOT_FOUND",
                        "Document not found",
                        HttpStatus.NOT_FOUND));
    }

    private ParsedDocument parse(KnowledgeDocument document) {
        try (InputStream inputStream = storage.open(document.storageKey())) {
            return parser.parse(inputStream, document.originalFilename(), document.mediaType());
        } catch (IOException exception) {
            throw new DocumentException(
                    "DOCUMENT_STORAGE_READ_FAILED",
                    "Failed to close the stored document stream",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void markFailed(UUID documentId, RuntimeException exception) {
        Instant failedAt = clock.instant();
        String message = exception.getMessage() == null ? "Document processing failed" : exception.getMessage();
        if (message.length() > MAX_ERROR_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        documentRepository.updateStatus(documentId, DocumentStatus.FAILED);
        taskRepository.markFailed(documentId, failedAt, message);
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String normalized = title.strip();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
