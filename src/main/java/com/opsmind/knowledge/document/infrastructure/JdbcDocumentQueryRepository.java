package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.DocumentPage;
import com.opsmind.knowledge.document.domain.DocumentQueryRepository;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.DocumentSummary;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcDocumentQueryRepository implements DocumentQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DocumentPage findPage(int page, int size) {
        long offset = (long) page * size;
        List<DocumentSummary> items = jdbcTemplate.query("""
                SELECT d.id, d.original_filename, d.media_type, d.size_bytes, d.sha256,
                       d.storage_key, d.status AS document_status, d.created_at,
                       t.id AS task_id, t.status AS task_status, t.error_message
                FROM knowledge_document d
                LEFT JOIN ingestion_task t ON t.document_id = d.id
                ORDER BY d.created_at DESC, d.id DESC
                LIMIT ? OFFSET ?
                """, this::mapSummary, size, offset);
        Long totalElements = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_document", Long.class);
        return new DocumentPage(items, page, size, totalElements == null ? 0 : totalElements);
    }

    private DocumentSummary mapSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        KnowledgeDocument document = new KnowledgeDocument(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("original_filename"),
                resultSet.getString("media_type"),
                resultSet.getLong("size_bytes"),
                resultSet.getString("sha256").trim(),
                resultSet.getString("storage_key"),
                DocumentStatus.valueOf(resultSet.getString("document_status")),
                resultSet.getTimestamp("created_at").toInstant());
        UUID taskId = resultSet.getObject("task_id", UUID.class);
        String taskStatus = resultSet.getString("task_status");
        return new DocumentSummary(
                document,
                taskId,
                taskStatus == null ? null : IngestionTaskStatus.valueOf(taskStatus),
                resultSet.getString("error_message"));
    }
}
