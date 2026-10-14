package com.opsmind.common.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

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
@RestControllerAdvice // 注解作用：对全部 REST 控制器统一处理异常。
public class GlobalExceptionHandler { // 类型作用：封装 GlobalExceptionHandler 对应的单一职责，供当前模块协作使用。

    // @ExceptionHandler 指定本方法专门处理哪一种异常；ResponseEntity 同时携带状态码和响应体。
    @ExceptionHandler(AgentException.class) // 注解作用：指定下面的方法负责处理这种异常。
    public ResponseEntity<ApiResponse<Void>> handleAgentException(AgentException exception) { // 方法作用：完成“handleAgentException”对应的内部步骤，供主流程复用。
        return ResponseEntity.status(exception.status())
                .body(ApiResponse.failure(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(DocumentException.class) // 注解作用：指定下面的方法负责处理这种异常。
    public ResponseEntity<ApiResponse<Void>> handleDocumentException(DocumentException exception) { // 方法作用：完成“handleDocumentException”对应的内部步骤，供主流程复用。
        return ResponseEntity.status(exception.status())
                .body(ApiResponse.failure(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(EvaluationException.class) // 注解作用：指定下面的方法负责处理这种异常。
    public ResponseEntity<ApiResponse<Void>> handleEvaluationException(EvaluationException exception) { // 方法作用：完成“handleEvaluationException”对应的内部步骤，供主流程复用。
        return ResponseEntity.status(exception.status())
                .body(ApiResponse.failure(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class) // 注解作用：指定下面的方法负责处理这种异常。
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded() { // 方法作用：完成“handleMaxUploadSizeExceeded”对应的内部步骤，供主流程复用。
        // 413 是 HTTP 标准中的 Payload Too Large。
        return ResponseEntity.status(413)
                .body(ApiResponse.failure("DOCUMENT_TOO_LARGE", "The uploaded file exceeds the configured limit"));
    }
}

