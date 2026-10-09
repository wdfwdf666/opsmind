package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.DocumentPage;
import com.opsmind.knowledge.document.domain.DocumentQueryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DocumentCatalogService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final DocumentQueryRepository queryRepository;

    public DocumentCatalogService(DocumentQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public DocumentPage list(int page, int size) {
        if (page < 0) {
            throw new DocumentException(
                    "DOCUMENT_PAGE_INVALID", "Page must be greater than or equal to 0", HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DocumentException(
                    "DOCUMENT_PAGE_SIZE_INVALID",
                    "Page size must be between 1 and " + MAX_PAGE_SIZE,
                    HttpStatus.BAD_REQUEST);
        }
        return queryRepository.findPage(page, size);
    }
}
