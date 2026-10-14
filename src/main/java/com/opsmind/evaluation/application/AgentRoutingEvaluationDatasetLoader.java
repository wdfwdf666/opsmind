package com.opsmind.evaluation.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationCase;
import com.opsmind.evaluation.domain.AgentRoutingEvaluationDataset;
import com.opsmind.evaluation.domain.EvaluationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
public class AgentRoutingEvaluationDatasetLoader { // 类型作用：评测数据加载器，读取固定 JSON 样本并校验格式。

    public static final String DEFAULT_DATASET = "opsmind-agent-routing-v1";
    private static final Map<String, String> DATASETS = Map.of(
            DEFAULT_DATASET, "evaluation/opsmind-agent-routing-v1.json");

    private final ObjectMapper objectMapper;

    public AgentRoutingEvaluationDatasetLoader() { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this(new ObjectMapper());
    }

    AgentRoutingEvaluationDatasetLoader(ObjectMapper objectMapper) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.objectMapper = objectMapper;
    }

    public AgentRoutingEvaluationDataset load(String requestedName) { // 方法作用：从受控资源读取数据并完成格式校验。
        String name = requestedName == null || requestedName.isBlank() ? DEFAULT_DATASET : requestedName.strip();
        String location = DATASETS.get(name);
        if (location == null) {
            throw new EvaluationException(
                    "EVALUATION_DATASET_NOT_FOUND", "Evaluation dataset not found: " + name, HttpStatus.NOT_FOUND);
        }
        try (var inputStream = new ClassPathResource(location).getInputStream()) {
            AgentRoutingEvaluationDataset dataset = objectMapper.readValue(
                    inputStream, AgentRoutingEvaluationDataset.class);
            validate(dataset, name);
            return dataset;
        } catch (IOException exception) {
            throw new EvaluationException(
                    "EVALUATION_DATASET_INVALID", "Evaluation dataset could not be loaded",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validate(AgentRoutingEvaluationDataset dataset, String requestedName) { // 方法作用：检查输入格式、大小和业务约束，非法时抛出明确异常。
        if (dataset == null || !requestedName.equals(dataset.name())
                || dataset.description() == null || dataset.description().isBlank()
                || dataset.cases() == null || dataset.cases().isEmpty()) {
            throw invalidDataset();
        }
        HashSet<String> ids = new HashSet<>();
        for (AgentRoutingEvaluationCase evaluationCase : dataset.cases()) {
            if (evaluationCase == null || evaluationCase.id() == null || evaluationCase.id().isBlank()
                    || !ids.add(evaluationCase.id())
                    || evaluationCase.category() == null || evaluationCase.category().isBlank()
                    || evaluationCase.query() == null || evaluationCase.query().isBlank()
                    || evaluationCase.expectedTaskType() == null) {
                throw invalidDataset();
            }
        }
    }

    private EvaluationException invalidDataset() { // 方法作用：完成“invalidDataset”对应的内部步骤，供主流程复用。
        return new EvaluationException(
                "EVALUATION_DATASET_INVALID", "Evaluation dataset is invalid", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
