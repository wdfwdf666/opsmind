package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.application.DocumentStorage;
import com.opsmind.knowledge.document.application.ValidatedDocument;
import com.opsmind.knowledge.document.config.DocumentProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class LocalDocumentStorage implements DocumentStorage {

    private final Path storageRoot;

    public LocalDocumentStorage(DocumentProperties properties) {
        this.storageRoot = properties.storageLocation().toAbsolutePath().normalize();
    }

    @Override
    public String store(ValidatedDocument document) {
        String storageKey = document.sha256() + "." + document.extension();
        Path target = storageRoot.resolve(storageKey).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new DocumentException("DOCUMENT_STORAGE_PATH_INVALID", "Invalid document storage path",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            Files.createDirectories(storageRoot);
            if (Files.exists(target)) {
                return storageKey;
            }
            Path temporary = Files.createTempFile(storageRoot, ".upload-", ".tmp");
            try {
                Files.write(temporary, document.content());
                moveIntoPlace(temporary, target);
            } finally {
                Files.deleteIfExists(temporary);
            }
            return storageKey;
        } catch (IOException exception) {
            throw new DocumentException("DOCUMENT_STORAGE_FAILED", "Failed to store the uploaded file",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void moveIntoPlace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            try {
                Files.move(temporary, target);
            } catch (FileAlreadyExistsException ignored) {
                // Another request stored the same content first.
            }
        } catch (FileAlreadyExistsException ignored) {
            // Another request stored the same content first.
        }
    }
}

