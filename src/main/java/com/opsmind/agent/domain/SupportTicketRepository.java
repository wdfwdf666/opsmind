package com.opsmind.agent.domain;

import java.util.Optional;
import java.util.UUID;

public interface SupportTicketRepository {

    SupportTicket create(SupportTicket ticket);

    Optional<SupportTicket> findByRunId(UUID runId);
}
