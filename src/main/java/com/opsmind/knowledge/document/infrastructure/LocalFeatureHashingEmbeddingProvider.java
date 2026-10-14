package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import com.opsmind.knowledge.document.application.EmbeddingProvider;
import com.opsmind.knowledge.document.config.EmbeddingProperties;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
@ConditionalOnProperty(prefix = "opsmind.embedding", name = "provider", havingValue = "local") // 注解作用：只有配置值满足条件时才启用当前实现。
public class LocalFeatureHashingEmbeddingProvider implements EmbeddingProvider { // 类型作用：封装 LocalFeatureHashingEmbeddingProvider 对应的单一职责，供当前模块协作使用。

    private final EmbeddingProperties properties;

    public LocalFeatureHashingEmbeddingProvider(EmbeddingProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.properties = properties;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public String model() { // 方法作用：返回当前实现使用的模型名称，供响应和审计展示。
        return properties.model();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public int dimension() { // 方法作用：完成“dimension”对应的内部步骤，供主流程复用。
        return properties.dimension();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public List<double[]> embed(List<String> texts) { // 方法作用：完成“embed”对应的内部步骤，供主流程复用。
        return texts.stream().map(this::embedOne).toList();
    }

    private double[] embedOne(String input) { // 方法作用：完成“embedOne”对应的内部步骤，供主流程复用。
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

    private List<String> features(String text) { // 方法作用：完成“features”对应的内部步骤，供主流程复用。
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

    private void normalize(double[] vector) { // 方法作用：完成“normalize”对应的内部步骤，供主流程复用。
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
