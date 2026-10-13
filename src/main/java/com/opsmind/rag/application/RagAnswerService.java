package com.opsmind.rag.application;

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

@Service
public class RagAnswerService {

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
            GenerationProperties properties) {
        this.searchService = searchService;
        this.generationProvider = generationProvider;
        this.properties = properties;
    }

    public RagAnswerResult answer(String query, Integer requestedTopK, UUID documentId) {
        int topK = requestedTopK == null ? properties.evidenceTopK() : requestedTopK;
        SemanticSearchResult searchResult = searchService.search(query, topK, documentId);
        Double topEvidenceScore = searchResult.hits().isEmpty() ? null : searchResult.hits().getFirst().score();
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
            String answer) {
        return new RagAnswerResult(
                searchResult.query(), documentId, searchResult.model(), generationProvider.model(), true,
                reason, topEvidenceScore, answer, List.of(), 0, 0, 0);
    }

    private List<RagEvidence> assembleEvidence(List<SemanticSearchHit> hits) {
        List<RagEvidence> evidence = new ArrayList<>();
        Set<UUID> seenParents = new LinkedHashSet<>();
        int remainingCharacters = properties.maxContextCharacters();
        for (SemanticSearchHit hit : hits) {
            UUID contextId = hit.parentId() == null ? hit.chunkId() : hit.parentId();
            if (!seenParents.add(contextId) || remainingCharacters <= 0) {
                continue;
            }
            String originalPassage = hit.parentText() == null || hit.parentText().isBlank()
                    ? hit.childText()
                    : hit.parentText();
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

    private String validateAnswer(GeneratedAnswer generated) {
        if (generated == null || generated.text() == null || generated.text().isBlank()) {
            throw providerError("empty answer");
        }
        return generated.text().strip();
    }

    private List<RagCitation> referencedCitations(String answer, List<RagEvidence> evidence) {
        Map<String, RagEvidence> bySourceId = new LinkedHashMap<>();
        evidence.forEach(item -> bySourceId.put(item.sourceId(), item));
        LinkedHashSet<String> referenced = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(answer);
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

    private String excerpt(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.strip();
        return normalized.length() <= EXCERPT_CHARACTERS
                ? normalized
                : normalized.substring(0, EXCERPT_CHARACTERS) + "…";
    }

    private DocumentException providerError(String reason) {
        return new DocumentException(
                "ANSWER_PROVIDER_INVALID_RESPONSE",
                "Answer provider returned an invalid response: " + reason,
                HttpStatus.BAD_GATEWAY);
    }
}
