package com.opsmind.knowledge.document.application;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class DocumentTextCleaner {

    public String clean(String source) {
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
