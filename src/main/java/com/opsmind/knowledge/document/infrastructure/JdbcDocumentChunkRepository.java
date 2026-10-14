package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

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

@Repository // 注解作用：标记数据库访问组件，并启用数据访问异常转换。
public class JdbcDocumentChunkRepository implements DocumentChunkRepository { // 类型作用：JDBC 仓储实现，使用参数化 SQL 读写数据库。

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentChunkRepository(JdbcTemplate jdbcTemplate) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    @Transactional // 注解作用：让方法在同一数据库事务中执行，异常时回滚。
    public void replaceAll(UUID documentId, List<DocumentChunk> chunks) { // 方法作用：更新数据或状态，并保持并发一致性。
        jdbcTemplate.update("DELETE FROM document_chunk WHERE document_id = ?", documentId);
        if (chunks.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO document_chunk
                    (id, document_id, parent_id, chunk_type, ordinal, heading_path, text_content,
                     content_hash, character_count, estimated_token_count, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, new BatchPreparedStatementSetter() { // 类型作用：封装  对应的单一职责，供当前模块协作使用。
            @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
            public void setValues(PreparedStatement statement, int index) throws SQLException { // 方法作用：完成“setValues”对应的内部步骤，供主流程复用。
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

            @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
            public int getBatchSize() { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
                return chunks.size();
            }
        });
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public DocumentChunkPage findPage(UUID documentId, ChunkType type, int page, int size) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
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

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public List<DocumentChunk> findAll(UUID documentId, ChunkType type) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return jdbcTemplate.query("""
                SELECT id, document_id, parent_id, chunk_type, ordinal, heading_path, text_content,
                       content_hash, character_count, estimated_token_count, created_at
                FROM document_chunk
                WHERE document_id = ? AND chunk_type = ?
                ORDER BY ordinal
                """, this::mapChunk, documentId, type.name());
    }

    private DocumentChunk mapChunk(ResultSet resultSet, int rowNumber) throws SQLException { // 方法作用：把外部或数据库数据转换成项目内部对象。
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
