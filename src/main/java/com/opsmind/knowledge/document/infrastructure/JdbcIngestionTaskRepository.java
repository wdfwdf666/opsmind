package com.opsmind.knowledge.document.infrastructure;

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

@Repository
public class JdbcIngestionTaskRepository implements IngestionTaskRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, document_id, status, created_at, updated_at, error_message
            FROM ingestion_task
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcIngestionTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public IngestionTask getOrCreate(UUID documentId, Instant now) {
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

    @Override
    public Optional<IngestionTask> findByDocumentId(UUID documentId) {
        List<IngestionTask> tasks = jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE document_id = ?", this::mapTask, documentId);
        return tasks.stream().findFirst();
    }

    private IngestionTask mapTask(ResultSet resultSet, int rowNumber) throws SQLException {
        return new IngestionTask(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                IngestionTaskStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getString("error_message"));
    }
}

