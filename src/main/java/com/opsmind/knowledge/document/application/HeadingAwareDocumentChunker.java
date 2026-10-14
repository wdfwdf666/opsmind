package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

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

/**
 * 按 Markdown 标题层级切分文档，并产生“父块 + 子块”。
 * <p>子块较短，适合向量检索定位；父块较长，命中子块后用于给模型提供更完整的语境。
 * 本类没有数据库和网络依赖，因此是一个容易单元测试的纯算法组件。</p>
 */
@Component // 注解作用：把普通组件交给 Spring 容器管理。
public class HeadingAwareDocumentChunker { // 类型作用：封装 HeadingAwareDocumentChunker 对应的单一职责，供当前模块协作使用。

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final int MAX_HEADING_PATH_LENGTH = 2000;

    private final ChunkingProperties properties;

    public HeadingAwareDocumentChunker(ChunkingProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.properties = properties;
    }

    public List<DocumentChunk> chunk(UUID documentId, String documentTitle, String text, Instant createdAt) { // 方法作用：完成“chunk”对应的内部步骤，供主流程复用。
        // 第一步先按标题解析章节，保留“文档 > 一级标题 > 二级标题”路径。
        List<Section> sections = parseSections(documentTitle, text);
        List<DocumentChunk> chunks = new ArrayList<>();
        int parentOrdinal = 0;
        int childOrdinal = 0;

        for (Section section : sections) {
            // 父块不重叠，控制给大模型的上下文大小。
            for (String parentText : split(section.text(), properties.parentMaxCharacters(), 0)) {
                String parentHash = sha256(parentText);
                UUID parentId = deterministicId(documentId, ChunkType.PARENT, parentOrdinal, parentHash);
                chunks.add(toChunk(
                        parentId, documentId, null, ChunkType.PARENT, parentOrdinal++, section.headingPath(),
                        parentText, parentHash, createdAt));

                // 子块保留少量重叠，减少答案恰好跨越切分边界时的信息丢失。
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

    private List<Section> parseSections(String documentTitle, String text) { // 方法作用：把外部或数据库数据转换成项目内部对象。
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

    private void addSection(List<Section> sections, String headingPath, StringBuilder text) { // 方法作用：完成“addSection”对应的内部步骤，供主流程复用。
        String content = text.toString().strip();
        if (!content.isBlank() && !isHeadingOnly(content)) {
            sections.add(new Section(headingPath, content));
        }
        text.setLength(0);
    }

    private boolean isHeadingOnly(String content) { // 方法作用：判断当前数据或状态是否满足指定条件。
        return !content.contains("\n") && MARKDOWN_HEADING.matcher(content).matches();
    }

    private String headingPath(String documentTitle, String[] headingStack) { // 方法作用：完成“headingPath”对应的内部步骤，供主流程复用。
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

    private String normalizedPath(String headingPath) { // 方法作用：完成“normalizedPath”对应的内部步骤，供主流程复用。
        if (headingPath == null || headingPath.isBlank()) {
            return null;
        }
        String normalized = headingPath.strip();
        return normalized.length() <= MAX_HEADING_PATH_LENGTH
                ? normalized
                : normalized.substring(0, MAX_HEADING_PATH_LENGTH);
    }

    private List<String> split(String text, int maxCharacters, int overlapCharacters) { // 方法作用：完成“split”对应的内部步骤，供主流程复用。
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

    private int preferredBreak(String text, int start, int proposedEnd) { // 方法作用：完成“preferredBreak”对应的内部步骤，供主流程复用。
        // 优先级：段落边界 > 换行 > 空格 > 硬切，尽量保持自然语义完整。
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
            Instant createdAt) { // 方法作用：把外部或数据库数据转换成项目内部对象。
        return new DocumentChunk(
                id, documentId, parentId, type, ordinal, headingPath, text, contentHash, text.length(),
                Math.max(1, (text.length() + 3) / 4), createdAt);
    }

    private UUID deterministicId(UUID documentId, ChunkType type, int ordinal, String contentHash) { // 方法作用：完成“deterministicId”对应的内部步骤，供主流程复用。
        // 使用稳定输入生成稳定 UUID：内容未变时重建仍得到相同 ID，便于缓存和排查。
        String key = documentId + ":" + type + ":" + ordinal + ":" + contentHash;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(String value) { // 方法作用：完成“sha256”对应的内部步骤，供主流程复用。
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record Section(String headingPath /* 字段作用：该不可变对象携带的 headingPath 数据。 */, String text /* 字段作用：本次流程使用的文本内容。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
    }
}
