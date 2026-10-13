package com.opsmind.agent.infrastructure;

import com.opsmind.agent.domain.SupportTicket;
import com.opsmind.agent.domain.SupportTicketRepository;
import com.opsmind.agent.domain.TicketPriority;
import com.opsmind.agent.domain.TicketStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSupportTicketRepository implements SupportTicketRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSupportTicketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SupportTicket create(SupportTicket ticket) {
        jdbcTemplate.update("""
                        INSERT INTO support_ticket
                            (id, agent_run_id, title, description, priority, status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                ticket.id(), ticket.runId(), ticket.title(), ticket.description(),
                ticket.priority().name(), ticket.status().name(), Timestamp.from(ticket.createdAt()));
        return ticket;
    }

    @Override
    public Optional<SupportTicket> findByRunId(UUID runId) {
        List<SupportTicket> tickets = jdbcTemplate.query("""
                        SELECT id, agent_run_id, title, description, priority, status, created_at
                        FROM support_ticket
                        WHERE agent_run_id = ?
                        """, this::mapTicket, runId);
        return tickets.stream().findFirst();
    }

    private SupportTicket mapTicket(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SupportTicket(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("agent_run_id", UUID.class),
                resultSet.getString("title"),
                resultSet.getString("description"),
                TicketPriority.valueOf(resultSet.getString("priority")),
                TicketStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
