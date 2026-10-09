package com.opsmind.knowledge.document.application;

import java.io.InputStream;

public interface DocumentParser {

    ParsedDocument parse(InputStream inputStream, String filename, String declaredMediaType);
}
