package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.application.EmbeddingProvider;
import com.opsmind.knowledge.document.config.EmbeddingProperties;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LocalFeatureHashingEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingProperties properties;

    public LocalFeatureHashingEmbeddingProvider(EmbeddingProperties properties) {
        this.properties = properties;
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public int dimension() {
        return properties.dimension();
    }

    @Override
    public List<double[]> embed(List<String> texts) {
        return texts.stream().map(this::embedOne).toList();
    }

    private double[] embedOne(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .strip();
        double[] vector = new double[dimension()];
        List<String> features = features(normalized);
        for (String feature : features) {
            int hash = feature.hashCode();
            int index = Math.floorMod(hash, vector.length);
            double weight = feature.codePointCount(0, feature.length()) == 1 ? 1.0 : 1.5;
            vector[index] += (hash & 1) == 0 ? weight : -weight;
        }
        normalize(vector);
        return vector;
    }

    private List<String> features(String text) {
        int[] codePoints = text.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .toArray();
        List<String> result = new ArrayList<>();
        for (int size = 1; size <= 3; size++) {
            for (int start = 0; start + size <= codePoints.length; start++) {
                result.add(new String(codePoints, start, size));
            }
        }
        return result;
    }

    private void normalize(double[] vector) {
        double squaredNorm = 0;
        for (double value : vector) {
            squaredNorm += value * value;
        }
        if (squaredNorm == 0) {
            return;
        }
        double norm = Math.sqrt(squaredNorm);
        for (int index = 0; index < vector.length; index++) {
            vector[index] /= norm;
        }
    }
}
