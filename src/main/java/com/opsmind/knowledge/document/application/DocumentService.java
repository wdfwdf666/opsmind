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

/**
 * “上传文档”用例的应用服务。
 * <p>它编排校验、内容去重、文件保存、元数据入库和任务创建，但不关心这些动作的具体技术实现。</p>
 */
// @Service 是 @Component 的业务语义版本；启动时 Spring 会创建单例对象并允许其他类注入它。
@Service
public class DocumentService {

    private final DocumentValidationService validationService;
    private final DocumentStorage storage;
    private final DocumentRepository documentRepository;
    private final IngestionTaskRepository taskRepository;
    private final Clock clock;

    // @Autowired 明确告诉 Spring 使用这个面向生产的构造器；Clock 使用系统 UTC 时间。
    @Autowired
    public DocumentService(
            DocumentValidationService validationService,
            DocumentStorage storage,
            DocumentRepository documentRepository,
            IngestionTaskRepository taskRepository) {
        this(validationService, storage, documentRepository, taskRepository, Clock.systemUTC());
    }

    // 包级可见构造器供测试传入固定 Clock，使时间相关断言稳定、可重复。
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
        // 先做大小、扩展名、真实文件签名和 SHA-256 计算，非法文件不会进入存储层。
        ValidatedDocument validated = validationService.validate(file);
        // 相同内容具有相同 SHA-256；先查重可以节省磁盘、解析和向量模型调用成本。
        var existing = documentRepository.findBySha256(validated.sha256());
        if (existing.isPresent()) {
            KnowledgeDocument document = existing.get();
            return new DocumentUploadResult(document, taskFor(document.id()), true);
        }

        // 原文件写入磁盘后，数据库只保存 storageKey，而不是把大文件塞进表中。
        String storageKey = storage.store(validated);
        Instant now = clock.instant();
        KnowledgeDocument candidate = new KnowledgeDocument(
                UUID.randomUUID(), validated.filename(), validated.mediaType(), validated.size(), validated.sha256(),
                storageKey, DocumentStatus.UPLOADED, now);
        // saveIfAbsent 由数据库唯一约束兜住并发上传：两个请求同时查不到时仍只会保存一份。
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

