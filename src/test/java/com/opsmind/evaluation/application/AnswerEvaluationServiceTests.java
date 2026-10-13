package com.opsmind.evaluation.application;

import com.opsmind.evaluation.domain.AnswerEvaluationCase;
import com.opsmind.evaluation.domain.AnswerEvaluationDataset;
import com.opsmind.evaluation.domain.EvaluationException;
import com.opsmind.rag.application.RagAnswerResult;
import com.opsmind.rag.application.RagAnswerService;
import com.opsmind.rag.application.RagCitation;
import com.opsmind.rag.application.RagRefusalReason;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnswerEvaluationServiceTests {

    private static final UUID DOCUMENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CHUNK_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void shouldCalculateAnswerCitationRefusalLatencyAndTokenMetrics() {
        AnswerEvaluationDatasetLoader loader = mock(AnswerEvaluationDatasetLoader.class);
        RagAnswerService answerService = mock(RagAnswerService.class);
        when(loader.load(null)).thenReturn(new AnswerEvaluationDataset(
                "answer-v1", "test", 3, List.of(
                new AnswerEvaluationCase(
                        "answerable", "检查什么", true,
                        List.of(List.of("活跃连接数"), List.of("慢查询")), List.of("告警处置")),
                new AnswerEvaluationCase(
                        "unanswerable", "未知制度", false, List.of(), List.of()))));
        when(answerService.answer("检查什么", 3, DOCUMENT_ID)).thenReturn(new RagAnswerResult(
                "检查什么", DOCUMENT_ID, "retrieval", "generation", false,
                null, 0.9, "检查活跃连接数和慢查询 [S1]",
                List.of(new RagCitation(
                        "S1", CHUNK_ID, null, DOCUMENT_ID, "manual.md", "告警处置",
                        "检查活跃连接数和慢查询", 0.9)),
                10, 4, 14));
        when(answerService.answer("未知制度", 3, DOCUMENT_ID)).thenReturn(new RagAnswerResult(
                "未知制度", DOCUMENT_ID, "retrieval", "generation", true,
                RagRefusalReason.NO_EVIDENCE, null,
                "未检索到可用的内部知识证据，暂时无法回答该问题。", List.of(), 0, 0, 0));
        long[] times = {0, 2_000_000, 10_000_000, 14_000_000};
        AtomicInteger timeIndex = new AtomicInteger();
        var service = new AnswerEvaluationService(loader, answerService, () -> times[timeIndex.getAndIncrement()]);

        var report = service.run(null, DOCUMENT_ID, null);

        assertThat(report.caseCount()).isEqualTo(2);
        assertThat(report.answerCorrectness()).isEqualTo(1.0);
        assertThat(report.faithfulnessProxy()).isEqualTo(1.0);
        assertThat(report.citationPrecision()).isEqualTo(1.0);
        assertThat(report.citationRecall()).isEqualTo(1.0);
        assertThat(report.refusalPrecision()).isEqualTo(1.0);
        assertThat(report.refusalRecall()).isEqualTo(1.0);
        assertThat(report.refusalF1()).isEqualTo(1.0);
        assertThat(report.p50LatencyMs()).isEqualTo(2.0);
        assertThat(report.p95LatencyMs()).isEqualTo(4.0);
        assertThat(report.totalTokens()).isEqualTo(14);
    }

    @Test
    void shouldExposeMissingRefusalAndPenalizeUnsupportedCitation() {
        AnswerEvaluationDatasetLoader loader = mock(AnswerEvaluationDatasetLoader.class);
        RagAnswerService answerService = mock(RagAnswerService.class);
        when(loader.load("answer-v1")).thenReturn(new AnswerEvaluationDataset(
                "answer-v1", "test", 3,
                List.of(new AnswerEvaluationCase("unknown", "未知制度", false, List.of(), List.of()))));
        when(answerService.answer("未知制度", 2, null)).thenReturn(new RagAnswerResult(
                "未知制度", null, "retrieval", "generation", false,
                null, 0.1, "错误回答 [S1]",
                List.of(new RagCitation(
                        "S1", CHUNK_ID, null, DOCUMENT_ID, "manual.md", "无关章节", "无关内容", 0.1)),
                1, 1, 2));
        var service = new AnswerEvaluationService(loader, answerService, () -> 1);

        var report = service.run("answer-v1", null, 2);

        assertThat(report.refusalRecall()).isZero();
        assertThat(report.refusalF1()).isZero();
        assertThat(report.citationPrecision()).isZero();
        assertThat(report.cases().getFirst().refused()).isFalse();
    }

    @Test
    void shouldRejectInvalidTopKBeforeCallingAnswerService() {
        AnswerEvaluationDatasetLoader loader = mock(AnswerEvaluationDatasetLoader.class);
        RagAnswerService answerService = mock(RagAnswerService.class);
        when(loader.load(null)).thenReturn(new AnswerEvaluationDataset(
                "answer-v1", "test", 3,
                List.of(new AnswerEvaluationCase("unknown", "未知制度", false, List.of(), List.of()))));
        var service = new AnswerEvaluationService(loader, answerService, System::nanoTime);

        assertThatThrownBy(() -> service.run(null, null, 0))
                .isInstanceOf(EvaluationException.class)
                .satisfies(exception -> assertThat(((EvaluationException) exception).code())
                        .isEqualTo("EVALUATION_TOP_K_INVALID"));
        verify(answerService, never()).answer(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
