package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.DocumentContent;
import com.opsmind.knowledge.document.domain.DocumentContentRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcDocumentContentRepository implements DocumentContentRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(DocumentContent content) {
        int updated = update(content);
        if (updated == 1) {
            return;
        }
        try {
            jdbcTemplate.update("""
                            INSERT INTO document_content
                                (document_id, title, detected_media_type, text_content, character_count, parsed_at)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    content.documentId(), content.title(), content.detectedMediaType(), content.text(),
                    content.characterCount(), Timestamp.from(content.parsedAt()));
        } catch (DuplicateKeyException duplicate) {
            update(content);
        }
    }

    @Override
    public Optional<DocumentContent> findByDocumentId(UUID documentId) {
        List<DocumentContent> contents = jdbcTemplate.query("""
                SELECT document_id, title, detected_media_type, text_content, character_count, parsed_at
                FROM document_content
                WHERE document_id = ?
                """, this::mapContent, documentId);
        return contents.stream().findFirst();
    }

    private int update(DocumentContent content) {
        return jdbcTemplate.update("""
                        UPDATE document_content
                        SET title = ?, detected_media_type = ?, text_content = ?, character_count = ?, parsed_at = ?
                        WHERE document_id = ?
                        """,
                content.title(), content.detectedMediaType(), content.text(), content.characterCount(),
                Timestamp.from(content.parsedAt()), content.documentId());
    }

    private DocumentContent mapContent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DocumentContent(
                resultSet.getObject("document_id", UUID.class),
                resultSet.getString("title"),
                resultSet.getString("detected_media_type"),
                resultSet.getString("text_content"),
                resultSet.getInt("character_count"),
                resultSet.getTimestamp("parsed_at").toInstant());
    }
}
