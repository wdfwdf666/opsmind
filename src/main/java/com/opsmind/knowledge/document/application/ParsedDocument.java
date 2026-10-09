package com.opsmind.knowledge.document.application;

public record ParsedDocument(
        String title,
        String detectedMediaType,
        String text) {
}
