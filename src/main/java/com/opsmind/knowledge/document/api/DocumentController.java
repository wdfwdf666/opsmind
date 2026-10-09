package com.opsmind.knowledge.document.api;

import com.opsmind.knowledge.document.application.DocumentService;
import com.opsmind.knowledge.document.application.DocumentCatalogService;
import com.opsmind.knowledge.document.application.DocumentChunkingService;
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

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Knowledge Documents", description = "Upload knowledge documents and inspect ingestion status")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentIngestionService ingestionService;
    private final DocumentCatalogService catalogService;
    private final DocumentChunkingService chunkingService;

    public DocumentController(
            DocumentService documentService,
            DocumentIngestionService ingestionService,
            DocumentCatalogService catalogService,
            DocumentChunkingService chunkingService) {
        this.documentService = documentService;
        this.ingestionService = ingestionService;
        this.catalogService = catalogService;
        this.chunkingService = chunkingService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
            @RequestPart("file") MultipartFile file) {
        DocumentUploadResult result = documentService.upload(file);
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .body(com.opsmind.common.api.ApiResponse.success(DocumentResponse.from(result)));
    }

    @GetMapping
    @Operation(
            summary = "List knowledge documents",
            description = "Returns documents ordered by creation time descending. Page indexes start at 0.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated document list"),
            @ApiResponse(responseCode = "400", description = "Invalid page or page size", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentPageResponse> list(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page, between 1 and 100", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return com.opsmind.common.api.ApiResponse.success(
                DocumentPageResponse.from(catalogService.list(page, size)));
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get document ingestion status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document and task status"),
            @ApiResponse(responseCode = "404", description = "Document was not found", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentResponse> find(
            @Parameter(description = "Document identifier", required = true,
                    schema = @Schema(format = "uuid"))
            @PathVariable UUID documentId) {
        return com.opsmind.common.api.ApiResponse.success(DocumentResponse.from(documentService.find(documentId)));
    }

    @PostMapping("/{documentId}/process")
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
            @PathVariable UUID documentId) {
        return com.opsmind.common.api.ApiResponse.success(
                DocumentResponse.from(ingestionService.process(documentId)));
    }

    @GetMapping("/{documentId}/content")
    @Operation(summary = "Get parsed and cleaned document content")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parsed content"),
            @ApiResponse(responseCode = "404", description = "Document was not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Content is not ready", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentContentResponse> content(
            @Parameter(description = "Document identifier", required = true, schema = @Schema(format = "uuid"))
            @PathVariable UUID documentId) {
        return com.opsmind.common.api.ApiResponse.success(
                DocumentContentResponse.from(ingestionService.findContent(documentId)));
    }

    @PostMapping("/{documentId}/chunks/rebuild")
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
            @PathVariable UUID documentId) {
        return com.opsmind.common.api.ApiResponse.success(
                DocumentChunkingResponse.from(chunkingService.rebuild(documentId)));
    }

    @GetMapping("/{documentId}/chunks")
    @Operation(summary = "List parent or child chunks")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated chunks"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination", content = @Content),
            @ApiResponse(responseCode = "404", description = "Document was not found", content = @Content)
    })
    public com.opsmind.common.api.ApiResponse<DocumentChunkPageResponse> chunks(
            @Parameter(description = "Document identifier", required = true, schema = @Schema(format = "uuid"))
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "CHILD") ChunkType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return com.opsmind.common.api.ApiResponse.success(
                DocumentChunkPageResponse.from(chunkingService.list(documentId, type, page, size)));
    }
}
