package com.opsmind.agent.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.agent.domain.AgentAuditRepository;
import com.opsmind.agent.domain.AgentRun;
import com.opsmind.agent.domain.AgentRunStatus;
import com.opsmind.agent.domain.AgentStep;
import com.opsmind.agent.domain.AgentStepType;
import com.opsmind.agent.domain.AgentToolRisk;
import com.opsmind.rag.application.RagAnswerResult;
import com.opsmind.rag.application.RagCitation;
import com.opsmind.rag.application.RagRefusalReason;
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
public class JdbcAgentAuditRepository implements AgentAuditRepository {

    private static final TypeReference<List<RagCitation>> CITATION_LIST = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcAgentAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(AgentRun run) {
        jdbcTemplate.update("""
                        INSERT INTO agent_run
                            (id, query_text, document_id, status, citations_json, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                run.id(), run.query(), run.documentId(), run.status().name(), "[]",
                Timestamp.from(run.createdAt()), Timestamp.from(run.updatedAt()));
    }

    @Override
    public void appendStep(AgentStep step) {
        jdbcTemplate.update("""
                        INSERT INTO agent_step
                            (id, run_id, sequence_no, step_type, tool_name, tool_risk, success, detail_json, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                step.id(), step.runId(), step.sequence(), step.type().name(), step.toolName(),
                step.toolRisk() == null ? null : step.toolRisk().name(), step.success(), step.detailJson(),
                Timestamp.from(step.createdAt()));
    }

    @Override
    public void complete(UUID runId, RagAnswerResult answer, Instant completedAt) {
        jdbcTemplate.update("""
                        UPDATE agent_run
                        SET status = ?, answer_text = ?, refused = ?, refusal_reason = ?, top_evidence_score = ?,
                            citations_json = ?, updated_at = ?, completed_at = ?
                        WHERE id = ?
                        """,
                AgentRunStatus.COMPLETED.name(), answer.answer(), answer.refused(),
                answer.refusalReason() == null ? null : answer.refusalReason().name(), answer.topEvidenceScore(),
                citationsJson(answer.citations()), Timestamp.from(completedAt), Timestamp.from(completedAt), runId);
    }

    @Override
    public void fail(UUID runId, String errorCode, String errorMessage, Instant completedAt) {
        jdbcTemplate.update("""
                        UPDATE agent_run
                        SET status = ?, error_code = ?, error_message = ?, updated_at = ?, completed_at = ?
                        WHERE id = ?
                        """,
                AgentRunStatus.FAILED.name(), errorCode, errorMessage,
                Timestamp.from(completedAt), Timestamp.from(completedAt), runId);
    }

    @Override
    public Optional<AgentRun> findRun(UUID runId) {
        List<AgentRun> runs = jdbcTemplate.query("""
                        SELECT id, query_text, document_id, status, answer_text, refused, refusal_reason,
                               top_evidence_score, citations_json, error_code, error_message,
                               created_at, updated_at, completed_at
                        FROM agent_run
                        WHERE id = ?
                        """, this::mapRun, runId);
        return runs.stream().findFirst();
    }

    @Override
    public List<AgentStep> findSteps(UUID runId) {
        return jdbcTemplate.query("""
                        SELECT id, run_id, sequence_no, step_type, tool_name, tool_risk,
                               success, detail_json, created_at
                        FROM agent_step
                        WHERE run_id = ?
                        ORDER BY sequence_no
                        """, this::mapStep, runId);
    }

    private AgentRun mapRun(ResultSet resultSet, int rowNumber) throws SQLException {
        String refusalReason = resultSet.getString("refusal_reason");
        Timestamp completedAt = resultSet.getTimestamp("completed_at");
        return new AgentRun(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("query_text"),
                resultSet.getObject("document_id", UUID.class),
                AgentRunStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("answer_text"),
                resultSet.getObject("refused", Boolean.class),
                refusalReason == null ? null : RagRefusalReason.valueOf(refusalReason),
                resultSet.getObject("top_evidence_score", Double.class),
                citations(resultSet.getString("citations_json")),
                resultSet.getString("error_code"),
                resultSet.getString("error_message"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                completedAt == null ? null : completedAt.toInstant());
    }

    private AgentStep mapStep(ResultSet resultSet, int rowNumber) throws SQLException {
        String risk = resultSet.getString("tool_risk");
        return new AgentStep(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("run_id", UUID.class),
                resultSet.getInt("sequence_no"),
                AgentStepType.valueOf(resultSet.getString("step_type")),
                resultSet.getString("tool_name"),
                risk == null ? null : AgentToolRisk.valueOf(risk),
                resultSet.getBoolean("success"),
                resultSet.getString("detail_json"),
                resultSet.getTimestamp("created_at").toInstant());
    }

    private String citationsJson(List<RagCitation> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize agent citations", exception);
        }
    }

    private List<RagCitation> citations(String json) {
        try {
            return objectMapper.readValue(json, CITATION_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize agent citations", exception);
        }
    }
}
