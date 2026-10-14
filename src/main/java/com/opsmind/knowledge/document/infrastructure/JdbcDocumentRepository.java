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

/**
 * DocumentRepository 的 PostgreSQL/JDBC 实现。
 * <p>应用层依赖接口，基础设施层用 JdbcTemplate 执行参数化 SQL。参数占位符 {@code ?}
 * 由驱动安全绑定，可避免手工拼接用户输入造成 SQL 注入。</p>
 */
// @Repository 表示数据库访问组件，并让 Spring 将部分 JDBC 异常转换为统一 DataAccessException。
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
            // sha256 唯一约束处理并发查重：后来者读取先写入的文档并报告 created=false。
            return findBySha256(document.sha256())
                    .map(existing -> new SaveResult(existing, false))
                    .orElseThrow(() -> duplicate);
        }
    }

    @Override
    public void updateStatus(UUID documentId, DocumentStatus status) {
        jdbcTemplate.update("UPDATE knowledge_document SET status = ? WHERE id = ?", status.name(), documentId);
    }

    private Optional<KnowledgeDocument> queryOne(String sql, Object argument) {
        List<KnowledgeDocument> documents = jdbcTemplate.query(sql, this::mapDocument, argument);
        return documents.stream().findFirst();
    }

    private KnowledgeDocument mapDocument(ResultSet resultSet, int rowNumber) throws SQLException {
        // RowMapper 把数据库当前行转换成领域对象；列名与 Flyway 建表脚本保持一致。
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

