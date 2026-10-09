package com.opsmind.knowledge.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opsmind.chunking")
public record ChunkingProperties(
        int parentMaxCharacters,
        int childMaxCharacters,
        int childOverlapCharacters) {

    public ChunkingProperties {
        if (parentMaxCharacters < 1 || childMaxCharacters < 1) {
            throw new IllegalArgumentException("Chunk sizes must be positive");
        }
        if (parentMaxCharacters < childMaxCharacters) {
            throw new IllegalArgumentException("Parent chunk size must not be smaller than child chunk size");
        }
        if (childOverlapCharacters < 0 || childOverlapCharacters >= childMaxCharacters) {
            throw new IllegalArgumentException("Child overlap must be between 0 and child chunk size - 1");
        }
    }
}
