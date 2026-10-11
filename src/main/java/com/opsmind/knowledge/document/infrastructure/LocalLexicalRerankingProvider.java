package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.application.ChineseNgramTokenizer;
import com.opsmind.knowledge.document.application.RerankingProvider;
import com.opsmind.knowledge.document.config.RerankingProperties;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "opsmind.reranking", name = "provider", havingValue = "local")
public class LocalLexicalRerankingProvider implements RerankingProvider {

    private final ChineseNgramTokenizer tokenizer;
    private final RerankingProperties properties;

    public LocalLexicalRerankingProvider(ChineseNgramTokenizer tokenizer, RerankingProperties properties) {
        this.tokenizer = tokenizer;
        this.properties = properties;
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public List<SemanticSearchHit> rerank(String query, List<SemanticSearchHit> candidates, int topK) {
        Set<String> queryTokens = new HashSet<>(tokenizer.tokenize(query));
        return candidates.stream()
                .map(hit -> withScore(hit, overlapScore(queryTokens, hit)))
                .sorted((left, right) -> {
                    int scoreOrder = Double.compare(right.score(), left.score());
                    return scoreOrder != 0 ? scoreOrder : left.chunkId().compareTo(right.chunkId());
                })
                .limit(topK)
                .toList();
    }

    private double overlapScore(Set<String> queryTokens, SemanticSearchHit hit) {
        Set<String> documentTokens = new HashSet<>(tokenizer.tokenize(
                (hit.headingPath() == null ? "" : hit.headingPath()) + "\n" + hit.childText()));
        if (queryTokens.isEmpty() || documentTokens.isEmpty()) {
            return 0;
        }
        long overlap = queryTokens.stream().filter(documentTokens::contains).count();
        return overlap / Math.sqrt((double) queryTokens.size() * documentTokens.size());
    }

    private SemanticSearchHit withScore(SemanticSearchHit hit, double score) {
        return new SemanticSearchHit(
                hit.chunkId(), hit.parentId(), hit.documentId(), hit.filename(), hit.headingPath(),
                hit.childText(), hit.parentText(), score);
    }
}
