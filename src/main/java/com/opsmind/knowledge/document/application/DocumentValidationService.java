package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.knowledge.document.config.DocumentProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service // 注解作用：把类注册为业务服务，由 Spring 负责创建和注入。
public class DocumentValidationService { // 类型作用：应用服务，负责组织一个完整业务流程。

    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "md", "text/markdown",
            "txt", "text/plain",
            "html", "text/html");

    private final DocumentProperties properties;

    public DocumentValidationService(DocumentProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.properties = properties;
    }

    public ValidatedDocument validate(MultipartFile file) { // 方法作用：检查输入格式、大小和业务约束，非法时抛出明确异常。
        if (file == null || file.isEmpty()) {
            throw error("DOCUMENT_EMPTY", "The uploaded file must not be empty", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > properties.maxSize().toBytes()) {
            throw error("DOCUMENT_TOO_LARGE", "The uploaded file exceeds the configured limit", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        String filename = sanitizeFilename(file.getOriginalFilename());
        String extension = extensionOf(filename);
        String mediaType = SUPPORTED_TYPES.get(extension);
        if (mediaType == null) {
            throw error("DOCUMENT_TYPE_NOT_SUPPORTED", "Supported file types: pdf, docx, md, txt, html",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        byte[] content = readContent(file);
        validateSignature(extension, content);
        return new ValidatedDocument(filename, extension, mediaType, content, sha256(content));
    }

    private String sanitizeFilename(String originalFilename) { // 方法作用：完成“sanitizeFilename”对应的内部步骤，供主流程复用。
        if (originalFilename == null || originalFilename.isBlank()) {
            throw error("DOCUMENT_FILENAME_INVALID", "The uploaded file must have a filename", HttpStatus.BAD_REQUEST);
        }
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (filename.isBlank() || filename.equals(".") || filename.equals("..")) {
            throw error("DOCUMENT_FILENAME_INVALID", "The uploaded filename is invalid", HttpStatus.BAD_REQUEST);
        }
        return filename;
    }

    private String extensionOf(String filename) { // 方法作用：完成“extensionOf”对应的内部步骤，供主流程复用。
        int separator = filename.lastIndexOf('.');
        if (separator < 1 || separator == filename.length() - 1) {
            return "";
        }
        return filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readContent(MultipartFile file) { // 方法作用：完成“readContent”对应的内部步骤，供主流程复用。
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new DocumentException("DOCUMENT_READ_FAILED", "Failed to read the uploaded file",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateSignature(String extension, byte[] content) { // 方法作用：完成“validateSignature”对应的内部步骤，供主流程复用。
        if (extension.equals("pdf") && !startsWith(content, "%PDF-".getBytes())) {
            throw error("DOCUMENT_SIGNATURE_INVALID", "The file content is not a valid PDF", HttpStatus.BAD_REQUEST);
        }
        if (extension.equals("docx") && !isDocx(content)) {
            throw error("DOCUMENT_SIGNATURE_INVALID", "The file content is not a valid DOCX", HttpStatus.BAD_REQUEST);
        }
        if (Set.of("md", "txt", "html").contains(extension) && containsNullByte(content)) {
            throw error("DOCUMENT_SIGNATURE_INVALID", "Text documents must not contain binary null bytes",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private boolean startsWith(byte[] content, byte[] signature) { // 方法作用：完成“startsWith”对应的内部步骤，供主流程复用。
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isDocx(byte[] content) { // 方法作用：判断当前数据或状态是否满足指定条件。
        boolean contentTypesFound = false;
        boolean documentXmlFound = false;
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                contentTypesFound |= entry.getName().equals("[Content_Types].xml");
                documentXmlFound |= entry.getName().equals("word/document.xml");
                if (contentTypesFound && documentXmlFound) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            return false;
        }
        return false;
    }

    private boolean containsNullByte(byte[] content) { // 方法作用：完成“containsNullByte”对应的内部步骤，供主流程复用。
        for (byte value : content) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    private String sha256(byte[] content) { // 方法作用：完成“sha256”对应的内部步骤，供主流程复用。
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private DocumentException error(String code, String message, HttpStatus status) { // 方法作用：完成“error”对应的内部步骤，供主流程复用。
        return new DocumentException(code, message, status);
    }
}

