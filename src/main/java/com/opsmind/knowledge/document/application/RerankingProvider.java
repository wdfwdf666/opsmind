package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.SemanticSearchHit;

import java.util.List;

public interface RerankingProvider {

    String model();

    List<SemanticSearchHit> rerank(String query, List<SemanticSearchHit> candidates, int topK);
}
