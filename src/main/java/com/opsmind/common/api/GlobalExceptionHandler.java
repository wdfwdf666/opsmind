package com.opsmind.common.api;

import com.opsmind.evaluation.domain.EvaluationException;
import com.opsmind.knowledge.document.domain.DocumentException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleDocumentException(DocumentException exception) {
        return ResponseEntity.status(exception.status())
                .body(ApiResponse.failure(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(EvaluationException.class)
    public ResponseEntity<ApiResponse<Void>> handleEvaluationException(EvaluationException exception) {
        return ResponseEntity.status(exception.status())
                .body(ApiResponse.failure(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded() {
        return ResponseEntity.status(413)
                .body(ApiResponse.failure("DOCUMENT_TOO_LARGE", "The uploaded file exceeds the configured limit"));
    }
}

