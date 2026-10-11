package com.opsmind.rag.application;

import java.util.List;

public interface AnswerGenerationProvider {

    String model();

    GeneratedAnswer generate(String query, List<RagEvidence> evidence);
}
