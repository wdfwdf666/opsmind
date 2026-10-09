package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.ChunkEmbedding;
import com.opsmind.knowledge.document.domain.ChunkEmbeddingRepository;
import com.opsmind.knowledge.document.domain.EmbeddedChunk;
import com.opsmind.knowledge.document.domain.NearestChunkSearch;
import com.opsmind.knowledge.document.domain.ScoredEmbeddedChunk;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcChunkEmbeddingRepository implements ChunkEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingVectorCodec vectorCodec;
    private final boolean postgresql;

    public JdbcChunkEmbeddingRepository(JdbcTemplate jdbcTemplate, EmbeddingVectorCodec vectorCodec) {
        this.jdbcTemplate = jdbcTemplate;
        this.vectorCodec = vectorCodec;
        this.postgresql = Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())));
    }

    @Override
    @Transactional
    public void replaceAll(UUID documentId, String model, List<ChunkEmbedding> embeddings) {
        jdbcTemplate.update(
                "DELETE FROM chunk_embedding WHERE document_id = ? AND embedding_model = ?", documentId, model);
        if (embeddings.isEmpty()) {
            return;
        }
        String sql = postgresql ? """
                INSERT INTO chunk_embedding
                    (chunk_id, document_id, embedding_model, dimension, vector_data, vector_value, content_hash, created_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS vector), ?, ?)
                """ : """
                INSERT INTO chunk_embedding
                    (chunk_id, document_id, embedding_model, dimension, vector_data, content_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                ChunkEmbedding embedding = embeddings.get(index);
                statement.setObject(1, embedding.chunkId());
                statement.setObject(2, embedding.documentId());
                statement.setString(3, embedding.model());
                statement.setInt(4, embedding.dimension());
                statement.setString(5, vectorCodec.encode(embedding.vector()));
                int offset = 0;
                if (postgresql) {
                    statement.setString(6, vectorCodec.encodePgVector(embedding.vector()));
                    offset = 1;
                }
                statement.setString(6 + offset, embedding.contentHash());
                statement.setTimestamp(7 + offset, Timestamp.from(embedding.createdAt()));
            }

            @Override
            public int getBatchSize() {
                return embeddings.size();
            }
        });
    }

    @Override
    public NearestChunkSearch findNearest(String model, double[] queryVector, UUID documentId, int limit) {
        return postgresql
                ? findNearestWithPgVector(model, queryVector, documentId, limit)
                : findNearestInMemory(model, queryVector, documentId, limit);
    }

    private NearestChunkSearch findNearestWithPgVector(
            String model, double[] queryVector, UUID documentId, int limit) {
        String documentFilter = documentId == null ? "" : " AND child.document_id = ?";
        DistanceSql distance = distanceSql(model, queryVector.length);
        int candidateCount = countCandidates(model, queryVector.length, documentId);
        String sql = """
                SELECT ranked.chunk_id, ranked.parent_id, ranked.document_id, ranked.filename,
                       ranked.heading_path, ranked.child_text, ranked.parent_text,
                       1 - ranked.distance AS score
                FROM (
                    SELECT child.id AS chunk_id, child.parent_id, child.document_id,
                           document.original_filename AS filename,
                           child.heading_path, child.text_content AS child_text,
                           parent.text_content AS parent_text,
                           %s AS distance
                    FROM chunk_embedding embedding
                    JOIN document_chunk child ON child.id = embedding.chunk_id
                    JOIN document_chunk parent ON parent.id = child.parent_id
                    JOIN knowledge_document document ON document.id = child.document_id
                    WHERE %s AND child.chunk_type = 'CHILD'
                """.formatted(distance.expression(), distance.modelPredicate())
                + documentFilter + " ORDER BY distance LIMIT ?) ranked ORDER BY ranked.distance";
        String encodedVector = vectorCodec.encodePgVector(queryVector);
        Object[] arguments = pgVectorArguments(distance.parameterizedModel(), encodedVector, model, documentId, limit);
        List<ScoredEmbeddedChunk> hits = jdbcTemplate.query(sql, (resultSet, rowNumber) ->
                new ScoredEmbeddedChunk(
                        resultSet.getObject("chunk_id", UUID.class),
                        resultSet.getObject("parent_id", UUID.class),
                        resultSet.getObject("document_id", UUID.class),
                        resultSet.getString("filename"),
                        resultSet.getString("heading_path"),
                        resultSet.getString("child_text"),
                        resultSet.getString("parent_text"),
                        resultSet.getDouble("score")), arguments);
        return new NearestChunkSearch(candidateCount, hits);
    }

    private int countCandidates(String model, int dimension, UUID documentId) {
        String documentFilter = documentId == null ? "" : " AND child.document_id = ?";
        String sql = """
                SELECT COUNT(*)
                FROM chunk_embedding embedding
                JOIN document_chunk child ON child.id = embedding.chunk_id
                WHERE embedding.embedding_model = ? AND embedding.dimension = ?
                  AND child.chunk_type = 'CHILD'
                """ + documentFilter;
        Integer count = documentId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, model, dimension)
                : jdbcTemplate.queryForObject(sql, Integer.class, model, dimension, documentId);
        return count == null ? 0 : count;
    }

    private Object[] pgVectorArguments(
            boolean parameterizedModel, String vector, String model, UUID documentId, int limit) {
        java.util.ArrayList<Object> arguments = new java.util.ArrayList<>();
        arguments.add(vector);
        if (parameterizedModel) {
            arguments.add(model);
        }
        if (documentId != null) {
            arguments.add(documentId);
        }
        arguments.add(limit);
        return arguments.toArray();
    }

    private DistanceSql distanceSql(String model, int dimension) {
        if ("qwen3.7-text-embedding".equals(model) && dimension == 1024) {
            return new DistanceSql(
                    "embedding.vector_value::vector(1024) <=> CAST(? AS vector(1024))",
                    "embedding.embedding_model = 'qwen3.7-text-embedding' AND embedding.dimension = 1024",
                    false);
        }
        if ("local-feature-hashing-v1".equals(model) && dimension == 256) {
            return new DistanceSql(
                    "embedding.vector_value::vector(256) <=> CAST(? AS vector(256))",
                    "embedding.embedding_model = 'local-feature-hashing-v1' AND embedding.dimension = 256",
                    false);
        }
        return new DistanceSql(
                "embedding.vector_value <=> CAST(? AS vector)",
                "embedding.embedding_model = ? AND embedding.dimension = " + dimension,
                true);
    }

    private NearestChunkSearch findNearestInMemory(
            String model, double[] queryVector, UUID documentId, int limit) {
        List<EmbeddedChunk> candidates = findCandidates(model, documentId);
        List<ScoredEmbeddedChunk> hits = candidates.stream()
                .map(candidate -> new ScoredEmbeddedChunk(
                        candidate.chunkId(), candidate.parentId(), candidate.documentId(), candidate.filename(),
                        candidate.headingPath(), candidate.childText(), candidate.parentText(),
                        cosine(queryVector, candidate.vector())))
                .sorted(Comparator.comparingDouble(ScoredEmbeddedChunk::score).reversed()
                        .thenComparing(hit -> hit.chunkId().toString()))
                .limit(limit)
                .toList();
        return new NearestChunkSearch(candidates.size(), hits);
    }

    private List<EmbeddedChunk> findCandidates(String model, UUID documentId) {
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

    private double cosine(double[] left, double[] right) {
        if (left.length != right.length) {
            return -1;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record DistanceSql(String expression, String modelPredicate, boolean parameterizedModel) {
    }

}
