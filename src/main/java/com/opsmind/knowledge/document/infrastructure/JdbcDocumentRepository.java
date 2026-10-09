package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.DocumentRepository;
import com.opsmind.knowledge.document.domain.DocumentStatus;
import com.opsmind.knowledge.document.domain.KnowledgeDocument;
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
public class JdbcDocumentRepository implements DocumentRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, original_filename, media_type, size_bytes, sha256, storage_key, status, created_at
            FROM knowledge_document
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<KnowledgeDocument> findById(UUID id) {
        return queryOne(SELECT_COLUMNS + " WHERE id = ?", id);
    }

    @Override
    public Optional<KnowledgeDocument> findBySha256(String sha256) {
        return queryOne(SELECT_COLUMNS + " WHERE sha256 = ?", sha256);
    }

    @Override
    public SaveResult saveIfAbsent(KnowledgeDocument document) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO knowledge_document
                                (id, original_filename, media_type, size_bytes, sha256, storage_key, status, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    document.id(), document.originalFilename(), document.mediaType(), document.size(), document.sha256(),
                    document.storageKey(), document.status().name(), Timestamp.from(document.createdAt()));
            return new SaveResult(document, true);
        } catch (DuplicateKeyException duplicate) {
            return findBySha256(document.sha256())
                    .map(existing -> new SaveResult(existing, false))
                    .orElseThrow(() -> duplicate);
        }
    }

    private Optional<KnowledgeDocument> queryOne(String sql, Object argument) {
        List<KnowledgeDocument> documents = jdbcTemplate.query(sql, this::mapDocument, argument);
        return documents.stream().findFirst();
    }

    private KnowledgeDocument mapDocument(ResultSet resultSet, int rowNumber) throws SQLException {
        return new KnowledgeDocument(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("original_filename"),
                resultSet.getString("media_type"),
                resultSet.getLong("size_bytes"),
                resultSet.getString("sha256").trim(),
                resultSet.getString("storage_key"),
                DocumentStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toInstant());
    }
}

