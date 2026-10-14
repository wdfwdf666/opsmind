package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.knowledge.document.application.DocumentService;
import com.opsmind.knowledge.document.application.DocumentCatalogService;
import com.opsmind.knowledge.document.application.DocumentChunkingService;
import com.opsmind.knowledge.document.application.DocumentEmbeddingService;
import com.opsmind.knowledge.document.application.DocumentIngestionService;
import com.opsmind.knowledge.document.application.DocumentUploadResult;
import com.opsmind.knowledge.document.domain.ChunkType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 文档管理接口：负责上传、解析、查看、分块和向量化。
 * <p>Controller 是 HTTP 层，不直接写 SQL，也不实现分块算法。它只做三件事：
 * 接收请求参数、调用应用服务、把结果包装成响应。这样的分层让业务逻辑可以脱离浏览器单独测试。</p>
 */
// 告诉 Spring MVC：这个类的方法用于处理 HTTP 请求，返回对象会由 Jackson 转成 JSON。
@RestController // 注解作用：把类注册为 REST 控制器，返回对象会自动转换成 JSON。
// 类级路径前缀；下面所有方法的地址都以 /api/v1/documents 开头。
@RequestMapping("/api/v1/documents") // 注解作用：声明该控制器所有接口共用的 URL 前缀。
// OpenAPI/Swagger 文档分组注解，只影响接口页面，不改变运行逻辑。
@Tag(name = "Knowledge Documents", description = "Upload knowledge documents and inspect ingestion status")
public class DocumentController { // 类型作用：HTTP 控制器，只负责收参、调用 Service 和返回 JSON。

    // final 表示依赖在构造后不再更换；这些对象由 Spring 容器创建并注入。
    private final DocumentService documentService;
    private final DocumentIngestionService ingestionService;
    private final DocumentCatalogService catalogService;
    private final DocumentChunkingService chunkingService;
    private final DocumentEmbeddingService embeddingService;

    /**
     * 构造器注入。只有一个构造器时不必写 {@code @Autowired}，Spring 会自动匹配参数类型。
     * 相比字段注入，构造器注入能保证对象创建后依赖完整，也更便于单元测试传入替身对象。
     */
    public DocumentController(
            DocumentService documentService,
            DocumentIngestionService ingestionService,
            DocumentCatalogService catalogService,
            DocumentChunkingService chunkingService,
            DocumentEmbeddingService embeddingService) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.documentService = documentService;
        this.ingestionService = ingestionService;
        this.catalogService = catalogService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
    }

    // @PostMapping 把 HTTP POST 映射到该方法；consumes 声明只接收表单文件上传格式。
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // 注解作用：把 HTTP POST 请求映射到下面的方法。
    @Operation(
            summary = "Upload a knowledge document",
            description = "Accepts PDF, DOCX, Markdown, TXT or HTML up to 20 MB. Duplicate content returns the existing document.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "A new document was accepted"),
            @ApiResponse(responseCode = "200", description = "Duplicate content; the existing document was returned"),
            @ApiResponse(responseCode = "400", description = "Empty, malformed or signature-mismatched file", content = @Content),
            @ApiResponse(responseCode = "413", description = "File exceeds the configured size limit", content = @Content),
            @ApiResponse(responseCode = "415", description = "File type is not supported", content = @Content)
    })
    public ResponseEntity<com.opsmind.common.api.ApiResponse<DocumentResponse>> upload(
            @Parameter(description = "Knowledge document to upload", required = true)
            // @RequestPart("file") 从 multipart 表单中读取名为 file 的部分。
            @RequestPart("file") MultipartFile file) { // 方法作用：校验并保存上传文件，内容重复时返回已有文档。
        DocumentUploadResult result = documentService.upload(file);
        // 新文档使用 201 Created；内容已存在时返回 200，体现去重结果。
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .body(com.opsmind.common.api.ApiResponse.success(DocumentResponse.from(result)));
    }

    // 没有子路径，因此对应 GET /api/v1/documents。
    @GetMapping // 注解作用：把 HTTP GET 请求映射到下面的方法。
    @Operation(
            summary = "List knowledge documents",
            description = "Returns documents ordered by creation time descending. Page indexes start at 0.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated document list"),
            @ApiResponse(responseCode = "400", description = "Invalid page or page size", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentPageResponse> list(
            @Parameter(description = "Zero-based page index", example = "0")
            // @RequestParam 读取 URL 查询参数；未传时由 defaultValue 提供默认值。
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page, between 1 and 100", example = "20")
            @RequestParam(defaultValue = "20") int size) { // 方法作用：按稳定顺序或分页条件列出数据。
        return com.opsmind.common.api.ApiResponse.success(
                DocumentPageResponse.from(catalogService.list(page, size)));
    }

    // {documentId} 是路径变量，例如 /api/v1/documents/550e8400-e29b-41d4-a716-446655440000。
    @GetMapping("/{documentId}") // 注解作用：把 HTTP GET 请求映射到下面的方法。
    @Operation(summary = "Get document ingestion status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document and task status"),
            @ApiResponse(responseCode = "404", description = "Document was not found", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentResponse> find(
            @Parameter(description = "Document identifier", required = true,
                    schema = @Schema(format = "uuid"))
            // Spring 会把路径中的字符串转换成 UUID；格式错误会在进入方法前返回 400。
            @PathVariable UUID documentId) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return com.opsmind.common.api.ApiResponse.success(DocumentResponse.from(documentService.find(documentId)));
    }

    // 使用 POST 是因为解析会修改数据库状态和内容，不是纯查询。
    @PostMapping("/{documentId}/process") // 注解作用：把 HTTP POST 请求映射到下面的方法。
    @Operation(
            summary = "Parse and clean an uploaded document",
            description = "Claims the pending ingestion task, extracts text with Apache Tika and persists cleaned content. "
                    + "Calling the operation after a failure retries the same task.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document content was parsed or was already available"),
            @ApiResponse(responseCode = "404", description = "Document was not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Document is already processing", content = @Content),
            @ApiResponse(responseCode = "422", description = "Document could not be parsed", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentResponse> process(
            @Parameter(description = "Document identifier", required = true, schema = @Schema(format = "uuid"))
            @PathVariable UUID documentId) { // 方法作用：解析原文、清洗正文并更新入库任务状态。
        return com.opsmind.common.api.ApiResponse.success(
                DocumentResponse.from(ingestionService.process(documentId)));
    }

    @GetMapping("/{documentId}/content") // 注解作用：把 HTTP GET 请求映射到下面的方法。
    @Operation(summary = "Get parsed and cleaned document content")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parsed content"),
            @ApiResponse(responseCode = "404", description = "Document was not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Content is not ready", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentContentResponse> content(
            @Parameter(description = "Document identifier", required = true, schema = @Schema(format = "uuid"))
            @PathVariable UUID documentId) { // 方法作用：完成“content”对应的内部步骤，供主流程复用。
        return com.opsmind.common.api.ApiResponse.success(
                DocumentContentResponse.from(ingestionService.findContent(documentId)));
    }

    // rebuild 表明这是一次可重复执行的重建操作，会原子替换旧分块。
    @PostMapping("/{documentId}/chunks/rebuild") // 注解作用：把 HTTP POST 请求映射到下面的方法。
    @Operation(
            summary = "Rebuild parent and child chunks",
            description = "Splits parsed content using Markdown headings and character budgets. "
                    + "The operation atomically replaces existing chunks and is idempotent for unchanged content.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chunks were rebuilt"),
            @ApiResponse(responseCode = "404", description = "Document was not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Parsed content is not ready", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentChunkingResponse> rebuildChunks(
            @Parameter(description = "Document identifier", required = true, schema = @Schema(format = "uuid"))
            @PathVariable UUID documentId) { // 方法作用：根据已解析正文重新生成父块和子块。
        return com.opsmind.common.api.ApiResponse.success(
                DocumentChunkingResponse.from(chunkingService.rebuild(documentId)));
    }

    @GetMapping("/{documentId}/chunks") // 注解作用：把 HTTP GET 请求映射到下面的方法。
    @Operation(summary = "List parent or child chunks")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated chunks"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination", content = @Content),
            @ApiResponse(responseCode = "404", description = "Document was not found", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentChunkPageResponse> chunks(
            @Parameter(description = "Document identifier", required = true, schema = @Schema(format = "uuid"))
            @PathVariable UUID documentId,
            // Spring 会按枚举名称把 CHILD/PARENT 转为 ChunkType。
            @RequestParam(defaultValue = "CHILD") ChunkType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) { // 方法作用：完成“chunks”对应的内部步骤，供主流程复用。
        return com.opsmind.common.api.ApiResponse.success(
                DocumentChunkPageResponse.from(chunkingService.list(documentId, type, page, size)));
    }

    // 只有 CHILD 分块需要向量化：小块利于精准召回，回答时再回溯父块获取完整上下文。
    @PostMapping("/{documentId}/embeddings/rebuild") // 注解作用：把 HTTP POST 请求映射到下面的方法。
    @Operation(
            summary = "Rebuild child chunk embeddings",
            description = "Embeds all child chunks with the configured model and atomically replaces that model's vectors.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Embeddings were rebuilt"),
            @ApiResponse(responseCode = "404", description = "Document was not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Child chunks are not ready", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentEmbeddingResponse> rebuildEmbeddings(
            @Parameter(description = "Document identifier", required = true, schema = @Schema(format = "uuid"))
            @PathVariable UUID documentId) { // 方法作用：重新计算所有子块向量并原子替换旧向量。
        return com.opsmind.common.api.ApiResponse.success(
                DocumentEmbeddingResponse.from(embeddingService.rebuild(documentId)));
    }
}
