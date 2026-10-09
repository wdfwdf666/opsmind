package com.opsmind.knowledge.document.domain;

import org.springframework.http.HttpStatus;

public class DocumentException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public DocumentException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}

