package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
public class ChineseNgramTokenizer { // 类型作用：封装 ChineseNgramTokenizer 对应的单一职责，供当前模块协作使用。

    public List<String> tokenize(String input) { // 方法作用：把外部或数据库数据转换成项目内部对象。
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        StringBuilder latin = new StringBuilder();
        List<String> hanRun = new ArrayList<>();
        normalized.codePoints().forEach(codePoint -> {
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                flushLatin(latin, tokens);
                hanRun.add(new String(Character.toChars(codePoint)));
            } else {
                flushHan(hanRun, tokens);
                if (Character.isLetterOrDigit(codePoint)) {
                    latin.appendCodePoint(codePoint);
                } else {
                    flushLatin(latin, tokens);
                }
            }
        });
        flushHan(hanRun, tokens);
        flushLatin(latin, tokens);
        return List.copyOf(tokens);
    }

    private void flushHan(List<String> run, List<String> tokens) { // 方法作用：完成“flushHan”对应的内部步骤，供主流程复用。
        if (run.isEmpty()) {
            return;
        }
        tokens.addAll(run);
        for (int index = 0; index + 1 < run.size(); index++) {
            tokens.add(run.get(index) + run.get(index + 1));
        }
        run.clear();
    }

    private void flushLatin(StringBuilder token, List<String> tokens) { // 方法作用：完成“flushLatin”对应的内部步骤，供主流程复用。
        if (!token.isEmpty()) {
            tokens.add(token.toString());
            token.setLength(0);
        }
    }
}
