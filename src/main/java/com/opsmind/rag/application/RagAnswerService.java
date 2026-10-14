package com.opsmind.rag.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.knowledge.document.application.RerankedSearchService;
import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import com.opsmind.knowledge.document.domain.SemanticSearchResult;
import com.opsmind.rag.config.GenerationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 核心编排服务。
 * <p>RAG 的关键不是“直接问模型”，而是先从企业知识库找到证据，再让模型严格依据证据作答，
 * 最后校验引用编号。这样可以降低幻觉，并让用户追溯答案来源。</p>
 */
@Service // 注解作用：把类注册为业务服务，由 Spring 负责创建和注入。
public class RagAnswerService { // 类型作用：应用服务，负责组织一个完整业务流程。

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[S(\\d+)]");
    private static final int EXCERPT_CHARACTERS = 240;
    private static final String NO_EVIDENCE_ANSWER = "未检索到可用的内部知识证据，暂时无法回答该问题。";
    private static final String LOW_RELEVANCE_ANSWER = "检索到的内部知识与问题相关性不足，暂时无法可靠回答。";

    private final RerankedSearchService searchService;
    private final AnswerGenerationProvider generationProvider;
    private final GenerationProperties properties;

    public RagAnswerService(
            RerankedSearchService searchService,
            AnswerGenerationProvider generationProvider,
            GenerationProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.searchService = searchService;
        this.generationProvider = generationProvider;
        this.properties = properties;
    }

    public RagAnswerResult answer(String query, Integer requestedTopK, UUID documentId) { // 方法作用：执行检索、证据门禁、上下文组装、模型生成和引用校验。
        int topK = requestedTopK == null ? properties.evidenceTopK() : requestedTopK;
        // documentId 为 null 时仓储不会增加文档过滤条件，即在整个知识库中自动选择相关内容。
        SemanticSearchResult searchResult = searchService.search(query, topK, documentId);
        Double topEvidenceScore = searchResult.hits().isEmpty() ? null : searchResult.hits().getFirst().score();
        // 没有证据或最高分过低时直接返回确定性结果，不调用大模型，节省成本并避免编造。
        if (topEvidenceScore == null) {
            return refusal(searchResult, documentId, RagRefusalReason.NO_EVIDENCE, null, NO_EVIDENCE_ANSWER);
        }
        if (topEvidenceScore < properties.minimumEvidenceScore()) {
            return refusal(
                    searchResult, documentId, RagRefusalReason.LOW_RELEVANCE,
                    topEvidenceScore, LOW_RELEVANCE_ANSWER);
        }
        List<RagEvidence> evidence = assembleEvidence(searchResult.hits());
        if (evidence.isEmpty()) {
            return refusal(searchResult, documentId, RagRefusalReason.NO_EVIDENCE, topEvidenceScore, NO_EVIDENCE_ANSWER);
        }

        // 只有证据门槛通过后才请求生成模型。
        GeneratedAnswer generated = generationProvider.generate(searchResult.query(), evidence);
        String answer = validateAnswer(generated);
        List<RagCitation> citations = referencedCitations(answer, evidence);
        return new RagAnswerResult(
                searchResult.query(), documentId, searchResult.model(), generationProvider.model(), false,
                null, topEvidenceScore, answer, citations,
                generated.promptTokens(), generated.completionTokens(), generated.totalTokens());
    }

    private RagAnswerResult refusal(
            SemanticSearchResult searchResult,
            UUID documentId,
            RagRefusalReason reason,
            Double topEvidenceScore,
            String answer) { // 方法作用：完成“refusal”对应的内部步骤，供主流程复用。
        return new RagAnswerResult(
                searchResult.query(), documentId, searchResult.model(), generationProvider.model(), true,
                reason, topEvidenceScore, answer, List.of(), 0, 0, 0);
    }

    private List<RagEvidence> assembleEvidence(List<SemanticSearchHit> hits) { // 方法作用：完成“assembleEvidence”对应的内部步骤，供主流程复用。
        List<RagEvidence> evidence = new ArrayList<>();
        Set<UUID> seenParents = new LinkedHashSet<>();
        int remainingCharacters = properties.maxContextCharacters();
        for (SemanticSearchHit hit : hits) {
            UUID contextId = hit.parentId() == null ? hit.chunkId() : hit.parentId();
            // 多个子块可能属于同一父块；父块去重防止上下文重复浪费 token。
            if (!seenParents.add(contextId) || remainingCharacters <= 0) {
                continue;
            }
            String originalPassage = hit.parentText() == null || hit.parentText().isBlank()
                    ? hit.childText()
                    : hit.parentText();
            // 用字符预算限制提示词体积，防止超过模型上下文窗口并控制费用。
            String passage = originalPassage.length() <= remainingCharacters
                    ? originalPassage
                    : originalPassage.substring(0, remainingCharacters);
            if (passage.isBlank()) {
                continue;
            }
            String sourceId = "S" + (evidence.size() + 1);
            evidence.add(new RagEvidence(
                    sourceId, hit.chunkId(), hit.parentId(), hit.documentId(), hit.filename(), hit.headingPath(),
                    passage, excerpt(hit.childText()), hit.score()));
            remainingCharacters -= passage.length();
        }
        return List.copyOf(evidence);
    }

    private String validateAnswer(GeneratedAnswer generated) { // 方法作用：完成“validateAnswer”对应的内部步骤，供主流程复用。
        if (generated == null || generated.text() == null || generated.text().isBlank()) {
            throw providerError("empty answer");
        }
        return generated.text().strip();
    }

    private List<RagCitation> referencedCitations(String answer, List<RagEvidence> evidence) { // 方法作用：完成“referencedCitations”对应的内部步骤，供主流程复用。
        Map<String, RagEvidence> bySourceId = new LinkedHashMap<>();
        evidence.forEach(item -> bySourceId.put(item.sourceId(), item));
        LinkedHashSet<String> referenced = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(answer);
        // 只接受本次提示词真实提供过的 [S1]、[S2]，模型虚构来源会触发 502。
        while (matcher.find()) {
            String sourceId = "S" + matcher.group(1);
            if (!bySourceId.containsKey(sourceId)) {
                throw providerError("unknown citation " + sourceId);
            }
            referenced.add(sourceId);
        }
        if (referenced.isEmpty()) {
            throw providerError("answer does not contain a source citation");
        }
        return referenced.stream().map(bySourceId::get).map(RagCitation::from).toList();
    }

    private String excerpt(String text) { // 方法作用：完成“excerpt”对应的内部步骤，供主流程复用。
        if (text == null) {
            return "";
        }
        String normalized = text.strip();
        return normalized.length() <= EXCERPT_CHARACTERS
                ? normalized
                : normalized.substring(0, EXCERPT_CHARACTERS) + "…";
    }

    private DocumentException providerError(String reason) { // 方法作用：完成“providerError”对应的内部步骤，供主流程复用。
        return new DocumentException(
                "ANSWER_PROVIDER_INVALID_RESPONSE",
                "Answer provider returned an invalid response: " + reason,
                HttpStatus.BAD_GATEWAY);
    }
}
