package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.config.ChunkingProperties;
import com.opsmind.knowledge.document.domain.ChunkType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class HeadingAwareDocumentChunkerTests {

    private final HeadingAwareDocumentChunker chunker = new HeadingAwareDocumentChunker(
            new ChunkingProperties(120, 50, 10));

    @Test
    void shouldCreateHeadingAwareParentAndOverlappingChildChunks() {
        UUID documentId = UUID.randomUUID();
        String text = "# 部署\n## 准备\n"
                + "检查数据库连接、磁盘空间和配置中心。".repeat(8)
                + "\n\n## 执行\n"
                + "先灰度发布，再检查监控指标，确认无错误后扩大流量。".repeat(8);

        var chunks = chunker.chunk(documentId, "运维手册", text, Instant.parse("2026-10-09T05:00:00Z"));
        var parents = chunks.stream().filter(chunk -> chunk.type() == ChunkType.PARENT).toList();
        var children = chunks.stream().filter(chunk -> chunk.type() == ChunkType.CHILD).toList();
        Set<UUID> parentIds = parents.stream().map(chunk -> chunk.id()).collect(Collectors.toSet());

        assertThat(parents).isNotEmpty().allSatisfy(parent -> {
            assertThat(parent.parentId()).isNull();
            assertThat(parent.characterCount()).isLessThanOrEqualTo(120);
        });
        assertThat(children).hasSizeGreaterThan(parents.size()).allSatisfy(child -> {
            assertThat(child.parentId()).isIn(parentIds);
            assertThat(child.characterCount()).isLessThanOrEqualTo(50);
            assertThat(child.estimatedTokenCount()).isPositive();
        });
        assertThat(chunks).anySatisfy(chunk ->
                assertThat(chunk.headingPath()).isEqualTo("运维手册 > 部署 > 准备"));
        assertThat(chunks).anySatisfy(chunk ->
                assertThat(chunk.headingPath()).isEqualTo("运维手册 > 部署 > 执行"));
    }

    @Test
    void shouldGenerateStableIdsForUnchangedContent() {
        UUID documentId = UUID.randomUUID();
        String text = "# 标题\n稳定内容";

        var first = chunker.chunk(documentId, null, text, Instant.parse("2026-10-09T05:00:00Z"));
        var second = chunker.chunk(documentId, null, text, Instant.parse("2026-10-09T06:00:00Z"));

        assertThat(second).extracting(chunk -> chunk.id())
                .containsExactlyElementsOf(first.stream().map(chunk -> chunk.id()).toList());
        assertThat(second).extracting(chunk -> chunk.contentHash())
                .containsExactlyElementsOf(first.stream().map(chunk -> chunk.contentHash()).toList());
    }
}
