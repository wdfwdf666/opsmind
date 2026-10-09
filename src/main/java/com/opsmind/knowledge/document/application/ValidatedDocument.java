package com.opsmind.knowledge.document.application;

public record ValidatedDocument(
        String filename,
        String extension,
        String mediaType,
        byte[] content,
        String sha256) {

    public long size() {
        return content.length;
    }
}

