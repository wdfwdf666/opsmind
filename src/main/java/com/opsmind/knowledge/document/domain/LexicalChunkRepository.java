package com.opsmind.knowledge.document.domain;

import java.util.List;
import java.util.UUID;

public interface LexicalChunkRepository {

    List<SearchableChunk> findCandidates(UUID documentId);
}
