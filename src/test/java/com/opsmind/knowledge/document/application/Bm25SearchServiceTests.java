package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.LexicalChunkRepository;
import com.opsmind.knowledge.document.domain.SearchableChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Bm25SearchServiceTests {

    @Test
    void shouldRankExactOperationalTermsAheadOfUnrelatedChunks() {
        UUID documentId = UUID.randomUUID();
        SearchableChunk rollback = chunk(documentId, "发布回滚", "验证回滚后错误率并检查数据库状态");
        SearchableChunk monitoring = chunk(documentId, "监控告警", "观察 CPU 和内存使用率");
        LexicalChunkRepository repository = mock(LexicalChunkRepository.class);
        when(repository.findCandidates(documentId)).thenReturn(List.of(monitoring, rollback));
        var service = new Bm25SearchService(repository, new ChineseNgramTokenizer());

        var result = service.search("数据库回滚怎么验证", documentId, 2);

        assertThat(result.model()).isEqualTo(Bm25SearchService.MODEL);
        assertThat(result.candidateCount()).isEqualTo(2);
        assertThat(result.hits()).extracting(hit -> hit.headingPath())
                .containsExactly("发布回滚");
        assertThat(result.hits().getFirst().score()).isPositive();
    }

    private SearchableChunk chunk(UUID documentId, String heading, String text) {
        return new SearchableChunk(
                UUID.randomUUID(), UUID.randomUUID(), documentId, "runbook.md", heading, text, text);
    }
}
