package com.opsmind.evaluation.domain;

import org.springframework.http.HttpStatus;

public class EvaluationException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public EvaluationException(String code, String message, HttpStatus status) {
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
