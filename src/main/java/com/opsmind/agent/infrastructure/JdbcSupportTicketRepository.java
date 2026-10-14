package com.opsmind.agent.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

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

@Repository // 注解作用：标记数据库访问组件，并启用数据访问异常转换。
public class JdbcSupportTicketRepository implements SupportTicketRepository { // 类型作用：JDBC 仓储实现，使用参数化 SQL 读写数据库。

    private final JdbcTemplate jdbcTemplate;

    public JdbcSupportTicketRepository(JdbcTemplate jdbcTemplate) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public SupportTicket create(SupportTicket ticket) { // 方法作用：创建或保存数据，并返回持久化结果。
        jdbcTemplate.update("""
                        INSERT INTO support_ticket
                            (id, agent_run_id, title, description, priority, status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                ticket.id(), ticket.runId(), ticket.title(), ticket.description(),
                ticket.priority().name(), ticket.status().name(), Timestamp.from(ticket.createdAt()));
        return ticket;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public Optional<SupportTicket> findByRunId(UUID runId) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        List<SupportTicket> tickets = jdbcTemplate.query("""
                        SELECT id, agent_run_id, title, description, priority, status, created_at
                        FROM support_ticket
                        WHERE agent_run_id = ?
                        """, this::mapTicket, runId);
        return tickets.stream().findFirst();
    }

    private SupportTicket mapTicket(ResultSet resultSet, int rowNumber) throws SQLException { // 方法作用：把外部或数据库数据转换成项目内部对象。
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
