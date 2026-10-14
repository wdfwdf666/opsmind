package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import com.opsmind.knowledge.document.domain.IngestionTask;
import com.opsmind.knowledge.document.domain.IngestionTaskRepository;
import com.opsmind.knowledge.document.domain.IngestionTaskStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository // 注解作用：标记数据库访问组件，并启用数据访问异常转换。
public class JdbcIngestionTaskRepository implements IngestionTaskRepository { // 类型作用：JDBC 仓储实现，使用参数化 SQL 读写数据库。

    private static final String SELECT_COLUMNS = """
            SELECT id, document_id, status, created_at, updated_at, error_message
            FROM ingestion_task
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcIngestionTaskRepository(JdbcTemplate jdbcTemplate) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public IngestionTask getOrCreate(UUID documentId, Instant now) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        Optional<IngestionTask> existing = findByDocumentId(documentId);
        if (existing.isPresent()) {
            return existing.get();
        }

        IngestionTask task = IngestionTask.pending(documentId, now);
        try {
            jdbcTemplate.update("""
                            INSERT INTO ingestion_task
                                (id, document_id, status, created_at, updated_at, error_message)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    task.id(), task.documentId(), task.status().name(), Timestamp.from(task.createdAt()),
                    Timestamp.from(task.updatedAt()), task.errorMessage());
            return task;
        } catch (DuplicateKeyException duplicate) {
            return findByDocumentId(documentId).orElseThrow(() -> duplicate);
        }
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public Optional<IngestionTask> findByDocumentId(UUID documentId) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        List<IngestionTask> tasks = jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE document_id = ?", this::mapTask, documentId);
        return tasks.stream().findFirst();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public boolean tryMarkRunning(UUID documentId, Instant now, Instant staleBefore) { // 方法作用：完成“tryMarkRunning”对应的内部步骤，供主流程复用。
        return jdbcTemplate.update("""
                UPDATE ingestion_task
                SET status = 'RUNNING', updated_at = ?, error_message = NULL
                WHERE document_id = ?
                  AND (status IN ('PENDING', 'FAILED') OR (status = 'RUNNING' AND updated_at < ?))
                """, Timestamp.from(now), documentId, Timestamp.from(staleBefore)) == 1;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public void markSucceeded(UUID documentId, Instant now) { // 方法作用：更新数据或状态，并保持并发一致性。
        updateStatus(documentId, IngestionTaskStatus.SUCCEEDED, now, null);
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public void markFailed(UUID documentId, Instant now, String errorMessage) { // 方法作用：更新数据或状态，并保持并发一致性。
        updateStatus(documentId, IngestionTaskStatus.FAILED, now, errorMessage);
    }

    private void updateStatus(UUID documentId, IngestionTaskStatus status, Instant now, String errorMessage) { // 方法作用：更新数据或状态，并保持并发一致性。
        jdbcTemplate.update("""
                UPDATE ingestion_task
                SET status = ?, updated_at = ?, error_message = ?
                WHERE document_id = ?
                """, status.name(), Timestamp.from(now), errorMessage, documentId);
    }

    private IngestionTask mapTask(ResultSet resultSet, int rowNumber) throws SQLException { // 方法作用：把外部或数据库数据转换成项目内部对象。
        return new IngestionTask(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                IngestionTaskStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getString("error_message"));
    }
}

