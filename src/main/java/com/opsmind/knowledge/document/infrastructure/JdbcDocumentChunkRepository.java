package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.ChunkType;
import com.opsmind.knowledge.document.domain.DocumentChunk;
import com.opsmind.knowledge.document.domain.DocumentChunkPage;
import com.opsmind.knowledge.document.domain.DocumentChunkRepository;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcDocumentChunkRepository implements DocumentChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void replaceAll(UUID documentId, List<DocumentChunk> chunks) {
        jdbcTemplate.update("DELETE FROM document_chunk WHERE document_id = ?", documentId);
        if (chunks.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO document_chunk
                    (id, document_id, parent_id, chunk_type, ordinal, heading_path, text_content,
                     content_hash, character_count, estimated_token_count, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                DocumentChunk chunk = chunks.get(index);
                statement.setObject(1, chunk.id());
                statement.setObject(2, chunk.documentId());
                statement.setObject(3, chunk.parentId());
                statement.setString(4, chunk.type().name());
                statement.setInt(5, chunk.ordinal());
                statement.setString(6, chunk.headingPath());
                statement.setString(7, chunk.text());
                statement.setString(8, chunk.contentHash());
                statement.setInt(9, chunk.characterCount());
                statement.setInt(10, chunk.estimatedTokenCount());
                statement.setTimestamp(11, Timestamp.from(chunk.createdAt()));
            }

            @Override
            public int getBatchSize() {
                return chunks.size();
            }
        });
    }

    @Override
    public DocumentChunkPage findPage(UUID documentId, ChunkType type, int page, int size) {
        long offset = (long) page * size;
        List<DocumentChunk> chunks = jdbcTemplate.query("""
                SELECT id, document_id, parent_id, chunk_type, ordinal, heading_path, text_content,
                       content_hash, character_count, estimated_token_count, created_at
                FROM document_chunk
                WHERE document_id = ? AND chunk_type = ?
                ORDER BY ordinal
                LIMIT ? OFFSET ?
                """, this::mapChunk, documentId, type.name(), size, offset);
        Long totalElements = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM document_chunk WHERE document_id = ? AND chunk_type = ?
                """, Long.class, documentId, type.name());
        return new DocumentChunkPage(
                chunks, type, page, size, totalElements == null ? 0 : totalElements);
    }

    private DocumentChunk mapChunk(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DocumentChunk(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                resultSet.getObject("parent_id", UUID.class),
                ChunkType.valueOf(resultSet.getString("chunk_type")),
                resultSet.getInt("ordinal"),
                resultSet.getString("heading_path"),
                resultSet.getString("text_content"),
                resultSet.getString("content_hash").trim(),
                resultSet.getInt("character_count"),
                resultSet.getInt("estimated_token_count"),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
