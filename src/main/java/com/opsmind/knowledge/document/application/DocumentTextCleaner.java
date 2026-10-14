package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
public class DocumentTextCleaner { // 类型作用：封装 DocumentTextCleaner 对应的单一职责，供当前模块协作使用。

    public String clean(String source) { // 方法作用：完成“clean”对应的内部步骤，供主流程复用。
        if (source == null || source.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFC)
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        String withoutControls = normalized.codePoints()
                .filter(character -> character == '\n' || character == '\t' || !Character.isISOControl(character))
                .mapToObj(Character::toString)
                .collect(Collectors.joining());

        String cleanedLines = Arrays.stream(withoutControls.split("\n", -1))
                .map(line -> line.replaceAll("[\\t ]+$", ""))
                .collect(Collectors.joining("\n"));
        return cleanedLines.replaceAll("\n{3,}", "\n\n").strip();
    }
}
