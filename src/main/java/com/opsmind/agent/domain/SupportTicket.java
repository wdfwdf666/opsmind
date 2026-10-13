package com.opsmind.agent.domain;

import java.time.Instant;
import java.util.UUID;

public record SupportTicket(
        UUID id,
        UUID runId,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        Instant createdAt) {
}
