package com.opsmind.knowledge.document.api;

import com.opsmind.common.api.ApiResponse;
import com.opsmind.knowledge.document.application.DocumentService;
import com.opsmind.knowledge.document.application.DocumentUploadResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(@RequestPart("file") MultipartFile file) {
        DocumentUploadResult result = documentService.upload(file);
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ApiResponse.success(DocumentResponse.from(result)));
    }

    @GetMapping("/{documentId}")
    public ApiResponse<DocumentResponse> find(@PathVariable UUID documentId) {
        return ApiResponse.success(DocumentResponse.from(documentService.find(documentId)));
    }
}
