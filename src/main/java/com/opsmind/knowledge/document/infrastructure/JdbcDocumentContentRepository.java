package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

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

@Repository // 注解作用：标记数据库访问组件，并启用数据访问异常转换。
public class JdbcDocumentContentRepository implements DocumentContentRepository { // 类型作用：JDBC 仓储实现，使用参数化 SQL 读写数据库。

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentContentRepository(JdbcTemplate jdbcTemplate) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public void save(DocumentContent content) { // 方法作用：创建或保存数据，并返回持久化结果。
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

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public Optional<DocumentContent> findByDocumentId(UUID documentId) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        List<DocumentContent> contents = jdbcTemplate.query("""
                SELECT document_id, title, detected_media_type, text_content, character_count, parsed_at
                FROM document_content
                WHERE document_id = ?
                """, this::mapContent, documentId);
        return contents.stream().findFirst();
    }

    private int update(DocumentContent content) { // 方法作用：更新数据或状态，并保持并发一致性。
        return jdbcTemplate.update("""
                        UPDATE document_content
                        SET title = ?, detected_media_type = ?, text_content = ?, character_count = ?, parsed_at = ?
                        WHERE document_id = ?
                        """,
                content.title(), content.detectedMediaType(), content.text(), content.characterCount(),
                Timestamp.from(content.parsedAt()), content.documentId());
    }

    private DocumentContent mapContent(ResultSet resultSet, int rowNumber) throws SQLException { // 方法作用：把外部或数据库数据转换成项目内部对象。
        return new DocumentContent(
                resultSet.getObject("document_id", UUID.class),
                resultSet.getString("title"),
                resultSet.getString("detected_media_type"),
                resultSet.getString("text_content"),
                resultSet.getInt("character_count"),
                resultSet.getTimestamp("parsed_at").toInstant());
    }
}
