package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

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

@Repository // 注解作用：标记数据库访问组件，并启用数据访问异常转换。
public class JdbcDocumentQueryRepository implements DocumentQueryRepository { // 类型作用：JDBC 仓储实现，使用参数化 SQL 读写数据库。

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentQueryRepository(JdbcTemplate jdbcTemplate) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public DocumentPage findPage(int page, int size) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
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

    private DocumentSummary mapSummary(ResultSet resultSet, int rowNumber) throws SQLException { // 方法作用：把外部或数据库数据转换成项目内部对象。
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
