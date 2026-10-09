package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.DocumentRepository;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.IngestionTask;
import com.opsmind.knowledge.document.domain.IngestionTaskRepository;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentValidationService validationService;
    private final DocumentStorage storage;
    private final DocumentRepository documentRepository;
    private final IngestionTaskRepository taskRepository;
    private final Clock clock;

    @Autowired
    public DocumentService(
            DocumentValidationService validationService,
            DocumentStorage storage,
            DocumentRepository documentRepository,
            IngestionTaskRepository taskRepository) {
        this(validationService, storage, documentRepository, taskRepository, Clock.systemUTC());
    }

    DocumentService(
            DocumentValidationService validationService,
            DocumentStorage storage,
            DocumentRepository documentRepository,
            IngestionTaskRepository taskRepository,
            Clock clock) {
        this.validationService = validationService;
        this.storage = storage;
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    public DocumentUploadResult upload(MultipartFile file) {
        ValidatedDocument validated = validationService.validate(file);
        var existing = documentRepository.findBySha256(validated.sha256());
        if (existing.isPresent()) {
            KnowledgeDocument document = existing.get();
            return new DocumentUploadResult(document, taskFor(document.id()), true);
        }

        String storageKey = storage.store(validated);
        Instant now = clock.instant();
        KnowledgeDocument candidate = new KnowledgeDocument(
                UUID.randomUUID(), validated.filename(), validated.mediaType(), validated.size(), validated.sha256(),
                storageKey, DocumentStatus.UPLOADED, now);
        DocumentRepository.SaveResult saved = documentRepository.saveIfAbsent(candidate);
        return new DocumentUploadResult(saved.document(), taskFor(saved.document().id()), !saved.created());
    }

    public DocumentUploadResult find(UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND));
        return new DocumentUploadResult(document, taskFor(document.id()), false);
    }

    private IngestionTask taskFor(UUID documentId) {
        return taskRepository.getOrCreate(documentId, clock.instant());
    }
}

