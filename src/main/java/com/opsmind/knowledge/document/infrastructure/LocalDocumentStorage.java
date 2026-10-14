package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import com.opsmind.knowledge.document.application.DocumentStorage;
import com.opsmind.knowledge.document.application.ValidatedDocument;
import com.opsmind.knowledge.document.config.DocumentProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
public class LocalDocumentStorage implements DocumentStorage { // 类型作用：封装 LocalDocumentStorage 对应的单一职责，供当前模块协作使用。

    private final Path storageRoot;

    public LocalDocumentStorage(DocumentProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.storageRoot = properties.storageLocation().toAbsolutePath().normalize();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public String store(ValidatedDocument document) { // 方法作用：创建或保存数据，并返回持久化结果。
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

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public InputStream open(String storageKey) { // 方法作用：完成“open”对应的内部步骤，供主流程复用。
        Path source = storageRoot.resolve(storageKey).normalize();
        if (!source.startsWith(storageRoot)) {
            throw new DocumentException("DOCUMENT_STORAGE_PATH_INVALID", "Invalid document storage path",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            return Files.newInputStream(source);
        } catch (IOException exception) {
            throw new DocumentException("DOCUMENT_FILE_NOT_FOUND", "Stored document file is unavailable",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void moveIntoPlace(Path temporary, Path target) throws IOException { // 方法作用：完成“moveIntoPlace”对应的内部步骤，供主流程复用。
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

