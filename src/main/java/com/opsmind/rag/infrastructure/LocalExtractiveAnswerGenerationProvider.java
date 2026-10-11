package com.opsmind.rag.infrastructure;

import com.opsmind.rag.application.AnswerGenerationProvider;
import com.opsmind.rag.application.GeneratedAnswer;
import com.opsmind.rag.application.RagEvidence;
import com.opsmind.rag.config.GenerationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "opsmind.generation", name = "provider", havingValue = "local")
public class LocalExtractiveAnswerGenerationProvider implements AnswerGenerationProvider {

    private final GenerationProperties properties;

    public LocalExtractiveAnswerGenerationProvider(GenerationProperties properties) {
        this.properties = properties;
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public GeneratedAnswer generate(String query, List<RagEvidence> evidence) {
        RagEvidence first = evidence.getFirst();
        return new GeneratedAnswer(first.excerpt() + " [" + first.sourceId() + "]", 0, 0, 0);
    }
}
