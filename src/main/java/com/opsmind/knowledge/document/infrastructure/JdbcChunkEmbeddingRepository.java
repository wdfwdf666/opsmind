package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.ChunkEmbedding;
import com.opsmind.knowledge.document.domain.ChunkEmbeddingRepository;
import com.opsmind.knowledge.document.domain.EmbeddedChunk;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcChunkEmbeddingRepository implements ChunkEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingVectorCodec vectorCodec;

    public JdbcChunkEmbeddingRepository(JdbcTemplate jdbcTemplate, EmbeddingVectorCodec vectorCodec) {
        this.jdbcTemplate = jdbcTemplate;
        this.vectorCodec = vectorCodec;
    }

    @Override
    @Transactional
    public void replaceAll(UUID documentId, String model, List<ChunkEmbedding> embeddings) {
        jdbcTemplate.update(
                "DELETE FROM chunk_embedding WHERE document_id = ? AND embedding_model = ?", documentId, model);
        if (embeddings.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO chunk_embedding
                    (chunk_id, document_id, embedding_model, dimension, vector_data, content_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                ChunkEmbedding embedding = embeddings.get(index);
                statement.setObject(1, embedding.chunkId());
                statement.setObject(2, embedding.documentId());
                statement.setString(3, embedding.model());
                statement.setInt(4, embedding.dimension());
                statement.setString(5, vectorCodec.encode(embedding.vector()));
                statement.setString(6, embedding.contentHash());
                statement.setTimestamp(7, Timestamp.from(embedding.createdAt()));
            }

            @Override
            public int getBatchSize() {
                return embeddings.size();
            }
        });
    }

    @Override
    public List<EmbeddedChunk> findCandidates(String model, UUID documentId) {
        String documentFilter = documentId == null ? "" : " AND child.document_id = ?";
        String sql = """
                SELECT child.id AS chunk_id, child.parent_id, child.document_id,
                       document.original_filename AS filename,
                       child.heading_path, child.text_content AS child_text,
                       parent.text_content AS parent_text, embedding.vector_data
                FROM chunk_embedding embedding
                JOIN document_chunk child ON child.id = embedding.chunk_id
                JOIN document_chunk parent ON parent.id = child.parent_id
                JOIN knowledge_document document ON document.id = child.document_id
                WHERE embedding.embedding_model = ? AND child.chunk_type = 'CHILD'
                """ + documentFilter + " ORDER BY child.document_id, child.ordinal";
        Object[] arguments = documentId == null ? new Object[]{model} : new Object[]{model, documentId};
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new EmbeddedChunk(
                resultSet.getObject("chunk_id", UUID.class),
                resultSet.getObject("parent_id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                resultSet.getString("filename"),
                resultSet.getString("heading_path"),
                resultSet.getString("child_text"),
                resultSet.getString("parent_text"),
                vectorCodec.decode(resultSet.getString("vector_data"))), arguments);
    }
}
