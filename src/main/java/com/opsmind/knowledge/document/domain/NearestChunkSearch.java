package com.opsmind.knowledge.document.domain;

import java.util.List;

public record NearestChunkSearch(int candidateCount, List<ScoredEmbeddedChunk> hits) {
}
