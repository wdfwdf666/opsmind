package com.opsmind.knowledge.document.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "opsmind.document")
public record DocumentProperties(
        DataSize maxSize,
        Path storageLocation) {
}

