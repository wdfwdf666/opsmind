package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.config.EmbeddingProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFeatureHashingEmbeddingProviderTests {

    private final LocalFeatureHashingEmbeddingProvider provider = new LocalFeatureHashingEmbeddingProvider(
            new EmbeddingProperties("local", "test-feature-hashing", 256, 5, 50, "", "", 20));

    @Test
    void shouldCreateDeterministicNormalizedVectorsAndRankRelatedTextHigher() {
        List<double[]> vectors = provider.embed(List.of(
                "数据库连接池泄漏",
                "数据库连接池耗尽需要排查连接泄漏",
                "发布前保存应用版本并执行灰度发布",
                "数据库连接池泄漏"));

        assertThat(vectors).allSatisfy(vector -> assertThat(vector).hasSize(256));
        assertThat(vectors.get(0)).containsExactly(vectors.get(3));
        assertThat(norm(vectors.get(0))).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(cosine(vectors.get(0), vectors.get(1)))
                .isGreaterThan(cosine(vectors.get(0), vectors.get(2)));
    }

    private double norm(double[] vector) {
        double sum = 0;
        for (double value : vector) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    private double cosine(double[] left, double[] right) {
        double sum = 0;
        for (int index = 0; index < left.length; index++) {
            sum += left[index] * right[index];
        }
        return sum;
    }
}
