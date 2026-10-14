package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import com.opsmind.knowledge.document.application.ChineseNgramTokenizer;
import com.opsmind.knowledge.document.application.RerankingProvider;
import com.opsmind.knowledge.document.config.RerankingProperties;
import com.opsmind.knowledge.document.domain.SemanticSearchHit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
@ConditionalOnProperty(prefix = "opsmind.reranking", name = "provider", havingValue = "local") // 注解作用：只有配置值满足条件时才启用当前实现。
public class LocalLexicalRerankingProvider implements RerankingProvider { // 类型作用：封装 LocalLexicalRerankingProvider 对应的单一职责，供当前模块协作使用。

    private final ChineseNgramTokenizer tokenizer;
    private final RerankingProperties properties;

    public LocalLexicalRerankingProvider(ChineseNgramTokenizer tokenizer, RerankingProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.tokenizer = tokenizer;
        this.properties = properties;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public String model() { // 方法作用：返回当前实现使用的模型名称，供响应和审计展示。
        return properties.model();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public List<SemanticSearchHit> rerank(String query, List<SemanticSearchHit> candidates, int topK) { // 方法作用：执行检索或相关性排序并返回有序结果。
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

    private double overlapScore(Set<String> queryTokens, SemanticSearchHit hit) { // 方法作用：完成“overlapScore”对应的内部步骤，供主流程复用。
        Set<String> documentTokens = new HashSet<>(tokenizer.tokenize(
                (hit.headingPath() == null ? "" : hit.headingPath()) + "\n" + hit.childText()));
        if (queryTokens.isEmpty() || documentTokens.isEmpty()) {
            return 0;
        }
        long overlap = queryTokens.stream().filter(documentTokens::contains).count();
        return overlap / Math.sqrt((double) queryTokens.size() * documentTokens.size());
    }

    private SemanticSearchHit withScore(SemanticSearchHit hit, double score) { // 方法作用：完成“withScore”对应的内部步骤，供主流程复用。
        return new SemanticSearchHit(
                hit.chunkId(), hit.parentId(), hit.documentId(), hit.filename(), hit.headingPath(),
                hit.childText(), hit.parentText(), score);
    }
}
