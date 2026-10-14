package com.opsmind.agent.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import org.springframework.http.HttpStatus;

public class AgentException extends RuntimeException { // 类型作用：业务异常，携带错误码和 HTTP 状态供全局异常处理器转换。

    private final String code;
    private final HttpStatus status;

    public AgentException(String code, String message, HttpStatus status) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() { // 方法作用：完成“code”对应的内部步骤，供主流程复用。
        return code;
    }

    public HttpStatus status() { // 方法作用：完成“status”对应的内部步骤，供主流程复用。
        return status;
    }
}
