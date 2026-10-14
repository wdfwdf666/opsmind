package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
public class EmbeddingVectorCodec { // 类型作用：封装 EmbeddingVectorCodec 对应的单一职责，供当前模块协作使用。

    public String encode(double[] vector) { // 方法作用：完成“encode”对应的内部步骤，供主流程复用。
        return Arrays.stream(vector)
                .mapToObj(Double::toString)
                .collect(Collectors.joining(","));
    }

    public String encodePgVector(double[] vector) { // 方法作用：完成“encodePgVector”对应的内部步骤，供主流程复用。
        return "[" + encode(vector) + "]";
    }

    public double[] decode(String encoded) { // 方法作用：完成“decode”对应的内部步骤，供主流程复用。
        if (encoded == null || encoded.isBlank()) {
            return new double[0];
        }
        return Arrays.stream(encoded.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }
}
