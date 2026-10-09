package com.opsmind.knowledge.document.domain;

public interface DocumentQueryRepository {

    DocumentPage findPage(int page, int size);
}
