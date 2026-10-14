package com.opsmind.common.api;

import com.opsmind.agent.domain.AgentException;
import com.opsmind.evaluation.domain.EvaluationException;
import com.opsmind.knowledge.document.domain.DocumentException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常翻译器：把 Java 异常转换成统一的 HTTP 响应。
 * <p>业务代码只需抛出包含 code、message、status 的异常；Spring MVC 会在异常冒出
 * Controller 后来到这里，从而保持正常流程代码简洁。</p>
 */
// @RestControllerAdvice = 对所有 @RestController 生效的增强组件，返回值会自动序列化为 JSON。
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @ExceptionHandler 指定本方法专门处理哪一种异常；ResponseEntity 同时携带状态码和响应体。
    @ExceptionHandler(AgentException.class)
    public ResponseEntity<ApiResponse<Void>> handleAgentException(AgentException exception) {
        return ResponseEntity.status(exception.status())
                .body(ApiResponse.failure(exception.code(), exception.getMessage()));
    }

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
        // 413 是 HTTP 标准中的 Payload Too Large。
        return ResponseEntity.status(413)
                .body(ApiResponse.failure("DOCUMENT_TOO_LARGE", "The uploaded file exceeds the configured limit"));
    }
}

