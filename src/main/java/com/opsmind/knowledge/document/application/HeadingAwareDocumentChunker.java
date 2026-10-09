package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.config.ChunkingProperties;
import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.DocumentChunk;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HeadingAwareDocumentChunker {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final int MAX_HEADING_PATH_LENGTH = 2000;

    private final ChunkingProperties properties;

    public HeadingAwareDocumentChunker(ChunkingProperties properties) {
        this.properties = properties;
    }

    public List<DocumentChunk> chunk(UUID documentId, String documentTitle, String text, Instant createdAt) {
        List<Section> sections = parseSections(documentTitle, text);
        List<DocumentChunk> chunks = new ArrayList<>();
        int parentOrdinal = 0;
        int childOrdinal = 0;

        for (Section section : sections) {
            for (String parentText : split(section.text(), properties.parentMaxCharacters(), 0)) {
                String parentHash = sha256(parentText);
                UUID parentId = deterministicId(documentId, ChunkType.PARENT, parentOrdinal, parentHash);
                chunks.add(toChunk(
                        parentId, documentId, null, ChunkType.PARENT, parentOrdinal++, section.headingPath(),
                        parentText, parentHash, createdAt));

                for (String childText : split(
                        parentText, properties.childMaxCharacters(), properties.childOverlapCharacters())) {
                    String childHash = sha256(childText);
                    UUID childId = deterministicId(documentId, ChunkType.CHILD, childOrdinal, childHash);
                    chunks.add(toChunk(
                            childId, documentId, parentId, ChunkType.CHILD, childOrdinal++, section.headingPath(),
                            childText, childHash, createdAt));
                }
            }
        }
        return List.copyOf(chunks);
    }

    private List<Section> parseSections(String documentTitle, String text) {
        List<Section> sections = new ArrayList<>();
        String[] headingStack = new String[6];
        StringBuilder currentText = new StringBuilder();
        String currentPath = normalizedPath(documentTitle);

        for (String line : text.split("\\n", -1)) {
            Matcher matcher = MARKDOWN_HEADING.matcher(line);
            if (matcher.matches()) {
                addSection(sections, currentPath, currentText);
                int level = matcher.group(1).length();
                headingStack[level - 1] = matcher.group(2).strip();
                Arrays.fill(headingStack, level, headingStack.length, null);
                currentPath = headingPath(documentTitle, headingStack);
            }
            currentText.append(line).append('\n');
        }
        addSection(sections, currentPath, currentText);
        return sections;
    }

    private void addSection(List<Section> sections, String headingPath, StringBuilder text) {
        String content = text.toString().strip();
        if (!content.isBlank() && !isHeadingOnly(content)) {
            sections.add(new Section(headingPath, content));
        }
        text.setLength(0);
    }

    private boolean isHeadingOnly(String content) {
        return !content.contains("\n") && MARKDOWN_HEADING.matcher(content).matches();
    }

    private String headingPath(String documentTitle, String[] headingStack) {
        List<String> elements = new ArrayList<>();
        if (documentTitle != null && !documentTitle.isBlank()) {
            elements.add(documentTitle.strip());
        }
        Arrays.stream(headingStack)
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .forEach(elements::add);
        return normalizedPath(String.join(" > ", elements));
    }

    private String normalizedPath(String headingPath) {
        if (headingPath == null || headingPath.isBlank()) {
            return null;
        }
        String normalized = headingPath.strip();
        return normalized.length() <= MAX_HEADING_PATH_LENGTH
                ? normalized
                : normalized.substring(0, MAX_HEADING_PATH_LENGTH);
    }

    private List<String> split(String text, int maxCharacters, int overlapCharacters) {
        if (text.length() <= maxCharacters) {
            return List.of(text);
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxCharacters, text.length());
            if (end < text.length()) {
                end = preferredBreak(text, start, end);
            }
            String part = text.substring(start, end).strip();
            if (!part.isBlank()) {
                parts.add(part);
            }
            if (end >= text.length()) {
                break;
            }
            int nextStart = Math.max(0, end - overlapCharacters);
            start = nextStart > start ? nextStart : end;
        }
        return parts;
    }

    private int preferredBreak(String text, int start, int proposedEnd) {
        int minimumBreak = start + (proposedEnd - start) / 2;
        int paragraphBreak = text.lastIndexOf("\n\n", proposedEnd);
        if (paragraphBreak >= minimumBreak) {
            return paragraphBreak + 2;
        }
        int lineBreak = text.lastIndexOf('\n', proposedEnd);
        if (lineBreak >= minimumBreak) {
            return lineBreak + 1;
        }
        int whitespace = text.lastIndexOf(' ', proposedEnd);
        return whitespace >= minimumBreak ? whitespace + 1 : proposedEnd;
    }

    private DocumentChunk toChunk(
            UUID id,
            UUID documentId,
            UUID parentId,
            ChunkType type,
            int ordinal,
            String headingPath,
            String text,
            String contentHash,
            Instant createdAt) {
        return new DocumentChunk(
                id, documentId, parentId, type, ordinal, headingPath, text, contentHash, text.length(),
                Math.max(1, (text.length() + 3) / 4), createdAt);
    }

    private UUID deterministicId(UUID documentId, ChunkType type, int ordinal, String contentHash) {
        String key = documentId + ":" + type + ":" + ordinal + ":" + contentHash;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record Section(String headingPath, String text) {
    }
}
