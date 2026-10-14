package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

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

/**
 * 文档解析流水线：领取任务 → 读取原文件 → Tika 提取文本 → 清洗 → 保存 → 更新状态。
 * <p>“任务表 + 租约”让失败任务可以重试，并避免多个请求同时处理同一文档。</p>
 */
@Service // 注解作用：把类注册为业务服务，由 Spring 负责创建和注入。
public class DocumentIngestionService { // 类型作用：应用服务，负责组织一个完整业务流程。

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;
    // RUNNING 超过 5 分钟可被重新领取，用于处理进程崩溃后遗留的“假运行”任务。
    private static final Duration PROCESSING_LEASE = Duration.ofMinutes(5);

    private final DocumentStorage storage;
    private final DocumentParser parser;
    private final DocumentTextCleaner cleaner;
    private final DocumentRepository documentRepository;
    private final IngestionTaskRepository taskRepository;
    private final DocumentContentRepository contentRepository;
    private final Clock clock;

    @Autowired // 注解作用：要求 Spring 按构造器参数类型注入依赖。
    public DocumentIngestionService(
            DocumentStorage storage,
            DocumentParser parser,
            DocumentTextCleaner cleaner,
            DocumentRepository documentRepository,
            IngestionTaskRepository taskRepository,
            DocumentContentRepository contentRepository) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this(storage, parser, cleaner, documentRepository, taskRepository, contentRepository, Clock.systemUTC());
    }

    DocumentIngestionService(
            DocumentStorage storage,
            DocumentParser parser,
            DocumentTextCleaner cleaner,
            DocumentRepository documentRepository,
            IngestionTaskRepository taskRepository,
            DocumentContentRepository contentRepository,
            Clock clock) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.storage = storage;
        this.parser = parser;
        this.cleaner = cleaner;
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.contentRepository = contentRepository;
        this.clock = clock;
    }

    public DocumentIngestionResult process(UUID documentId) { // 方法作用：解析原文、清洗正文并更新入库任务状态。
        KnowledgeDocument document = requireDocument(documentId);
        // getOrCreate 使接口具有幂等性：重复调用继续使用同一任务，而不是无限创建任务。
        IngestionTask task = taskRepository.getOrCreate(documentId, clock.instant());
        if (task.status() == IngestionTaskStatus.SUCCEEDED) {
            return completedResult(documentId);
        }
        Instant startedAt = clock.instant();
        // tryMarkRunning 在 SQL 层做原子状态更新；返回 false 表示任务已被其他线程持有。
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
            // 先保存内容，再把文档和任务标记成功，避免状态显示 READY 却查不到内容。
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
            // 统一记录失败状态，后续调用 process 可以看到原因并发起重试。
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

    public DocumentContent findContent(UUID documentId) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        requireDocument(documentId);
        return contentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentException(
                        "DOCUMENT_CONTENT_NOT_READY",
                        "Document content is not ready",
                        HttpStatus.CONFLICT));
    }

    private DocumentIngestionResult completedResult(UUID documentId) { // 方法作用：完成“completedResult”对应的内部步骤，供主流程复用。
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

    private KnowledgeDocument requireDocument(UUID documentId) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(
                        "DOCUMENT_NOT_FOUND",
                        "Document not found",
                        HttpStatus.NOT_FOUND));
    }

    private ParsedDocument parse(KnowledgeDocument document) { // 方法作用：把外部或数据库数据转换成项目内部对象。
        try (InputStream inputStream = storage.open(document.storageKey())) {
            return parser.parse(inputStream, document.originalFilename(), document.mediaType());
        } catch (IOException exception) {
            throw new DocumentException(
                    "DOCUMENT_STORAGE_READ_FAILED",
                    "Failed to close the stored document stream",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void markFailed(UUID documentId, RuntimeException exception) { // 方法作用：更新数据或状态，并保持并发一致性。
        Instant failedAt = clock.instant();
        String message = exception.getMessage() == null ? "Document processing failed" : exception.getMessage();
        if (message.length() > MAX_ERROR_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        documentRepository.updateStatus(documentId, DocumentStatus.FAILED);
        taskRepository.markFailed(documentId, failedAt, message);
    }

    private String normalizeTitle(String title) { // 方法作用：完成“normalizeTitle”对应的内部步骤，供主流程复用。
        if (title == null || title.isBlank()) {
            return null;
        }
        String normalized = title.strip();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
