package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

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
@Repository // 注解作用：标记数据库访问组件，并启用数据访问异常转换。
public class JdbcDocumentRepository implements DocumentRepository { // 类型作用：JDBC 仓储实现，使用参数化 SQL 读写数据库。

    private static final String SELECT_COLUMNS = """
            SELECT id, original_filename, media_type, size_bytes, sha256, storage_key, status, created_at
            FROM knowledge_document
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentRepository(JdbcTemplate jdbcTemplate) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public Optional<KnowledgeDocument> findById(UUID id) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return queryOne(SELECT_COLUMNS + " WHERE id = ?", id);
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public Optional<KnowledgeDocument> findBySha256(String sha256) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        return queryOne(SELECT_COLUMNS + " WHERE sha256 = ?", sha256);
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public SaveResult saveIfAbsent(KnowledgeDocument document) { // 方法作用：创建或保存数据，并返回持久化结果。
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

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public void updateStatus(UUID documentId, DocumentStatus status) { // 方法作用：更新数据或状态，并保持并发一致性。
        jdbcTemplate.update("UPDATE knowledge_document SET status = ? WHERE id = ?", status.name(), documentId);
    }

    private Optional<KnowledgeDocument> queryOne(String sql, Object argument) { // 方法作用：完成“queryOne”对应的内部步骤，供主流程复用。
        List<KnowledgeDocument> documents = jdbcTemplate.query(sql, this::mapDocument, argument);
        return documents.stream().findFirst();
    }

    private KnowledgeDocument mapDocument(ResultSet resultSet, int rowNumber) throws SQLException { // 方法作用：把外部或数据库数据转换成项目内部对象。
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

