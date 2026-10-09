package com.opsmind.knowledge.document.application;

import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.DocumentPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentCatalogServiceTests {

    @Test
    void shouldReturnRepositoryPage() {
        DocumentCatalogService service = new DocumentCatalogService(
                (page, size) -> new DocumentPage(List.of(), page, size, 41));

        DocumentPage result = service.list(1, 20);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(41);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void shouldRejectInvalidPagination() {
        DocumentCatalogService service = new DocumentCatalogService(
                (page, size) -> new DocumentPage(List.of(), page, size, 0));

        assertThatThrownBy(() -> service.list(-1, 20))
                .isInstanceOfSatisfying(DocumentException.class,
                        exception -> assertThat(exception.code()).isEqualTo("DOCUMENT_PAGE_INVALID"));
        assertThatThrownBy(() -> service.list(0, 0))
                .isInstanceOfSatisfying(DocumentException.class,
                        exception -> assertThat(exception.code()).isEqualTo("DOCUMENT_PAGE_SIZE_INVALID"));
        assertThatThrownBy(() -> service.list(0, 101))
                .isInstanceOfSatisfying(DocumentException.class,
                        exception -> assertThat(exception.code()).isEqualTo("DOCUMENT_PAGE_SIZE_INVALID"));
    }
}
