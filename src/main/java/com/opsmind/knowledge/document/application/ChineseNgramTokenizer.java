package com.opsmind.knowledge.document.application;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ChineseNgramTokenizer {

    public List<String> tokenize(String input) {
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

    private void flushHan(List<String> run, List<String> tokens) {
        if (run.isEmpty()) {
            return;
        }
        tokens.addAll(run);
        for (int index = 0; index + 1 < run.size(); index++) {
            tokens.add(run.get(index) + run.get(index + 1));
        }
        run.clear();
    }

    private void flushLatin(StringBuilder token, List<String> tokens) {
        if (!token.isEmpty()) {
            tokens.add(token.toString());
            token.setLength(0);
        }
    }
}
