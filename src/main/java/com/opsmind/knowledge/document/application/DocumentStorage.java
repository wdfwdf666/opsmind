package com.opsmind.knowledge.document.application;

import java.io.InputStream;

public interface DocumentStorage {

    String store(ValidatedDocument document);

    InputStream open(String storageKey);
}

