package com.opsmind.knowledge.document.application;

import java.util.List;

public interface EmbeddingProvider {

    String model();

    int dimension();

    List<double[]> embed(List<String> texts);
}
