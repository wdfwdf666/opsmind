package com.opsmind.rag.application;

public record GeneratedAnswer(
        String text,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens) {
}
