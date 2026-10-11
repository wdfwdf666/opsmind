package com.opsmind.rag.application;

import com.opsmind.knowledge.document.application.RerankedSearchService;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import com.opsmind.rag.config.GenerationProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagAnswerServiceTests {

    @Test
    void shouldReturnOnlyCitationsReferencedByGeneratedAnswer() {
        UUID documentId = UUID.randomUUID();
        RerankedSearchService search = mock(RerankedSearchService.class);
        AnswerGenerationProvider provider = mock(AnswerGenerationProvider.class);
        SemanticSearchHit first = hit(documentId, UUID.randomUUID(), "告警处置", "第一段");
        SemanticSearchHit second = hit(documentId, UUID.randomUUID(), "发布回滚", "第二段");
        when(search.search("怎么处置", 2, documentId)).thenReturn(new SemanticSearchResult(
                "怎么处置", "retrieval-model", documentId, 2, List.of(first, second)));
        when(provider.model()).thenReturn("generation-model");
        when(provider.generate(org.mockito.ArgumentMatchers.eq("怎么处置"), anyList()))
                .thenReturn(new GeneratedAnswer("应先检查告警指标 [S1]。", 100, 20, 120));
        RagAnswerService service = new RagAnswerService(search, provider, properties());

        RagAnswerResult result = service.answer("怎么处置", 2, documentId);

        assertThat(result.refused()).isFalse();
        assertThat(result.answer()).contains("[S1]");
        assertThat(result.citations()).singleElement()
                .satisfies(citation -> assertThat(citation.headingPath()).isEqualTo("告警处置"));
        assertThat(result.totalTokens()).isEqualTo(120);
    }

    @Test
    void shouldRefuseWithoutCallingGeneratorWhenRetrievalHasNoEvidence() {
        RerankedSearchService search = mock(RerankedSearchService.class);
        AnswerGenerationProvider provider = mock(AnswerGenerationProvider.class);
        when(provider.model()).thenReturn("generation-model");
        when(search.search("unknown", 5, null)).thenReturn(new SemanticSearchResult(
                "unknown", "retrieval-model", null, 0, List.of()));
        RagAnswerService service = new RagAnswerService(search, provider, properties());

        RagAnswerResult result = service.answer("unknown", null, null);

        assertThat(result.refused()).isTrue();
        assertThat(result.citations()).isEmpty();
        assertThat(result.answer()).contains("未检索到");
        verify(provider, never()).generate(org.mockito.ArgumentMatchers.anyString(), anyList());
    }

    @Test
    void shouldRejectCitationThatDoesNotExistInEvidence() {
        UUID documentId = UUID.randomUUID();
        RerankedSearchService search = mock(RerankedSearchService.class);
        AnswerGenerationProvider provider = mock(AnswerGenerationProvider.class);
        when(search.search("query", 1, documentId)).thenReturn(new SemanticSearchResult(
                "query", "retrieval-model", documentId, 1,
                List.of(hit(documentId, UUID.randomUUID(), "章节", "证据"))));
        when(provider.generate(org.mockito.ArgumentMatchers.eq("query"), anyList()))
                .thenReturn(new GeneratedAnswer("错误引用 [S2]", 10, 5, 15));
        RagAnswerService service = new RagAnswerService(search, provider, properties());

        assertThatThrownBy(() -> service.answer("query", 1, documentId))
                .isInstanceOf(DocumentException.class)
                .satisfies(exception -> assertThat(((DocumentException) exception).code())
                        .isEqualTo("ANSWER_PROVIDER_INVALID_RESPONSE"));
    }

    private GenerationProperties properties() {
        return new GenerationProperties("local", "generation-model", "", "", 5, 12000, 1200, 0.1);
    }

    private SemanticSearchHit hit(UUID documentId, UUID parentId, String heading, String child) {
        return new SemanticSearchHit(
                UUID.randomUUID(), parentId, documentId, "runbook.md", heading,
                child, "完整父级上下文：" + child, 0.9);
    }
}
