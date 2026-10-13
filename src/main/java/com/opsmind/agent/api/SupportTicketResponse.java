package com.opsmind.agent.api;

import com.opsmind.agent.domain.SupportTicket;
import com.opsmind.agent.domain.TicketPriority;
import com.opsmind.agent.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record SupportTicketResponse(
        UUID ticketId,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        Instant createdAt) {

    static SupportTicketResponse from(SupportTicket ticket) {
        if (ticket == null) {
            return null;
        }
        return new SupportTicketResponse(
                ticket.id(), ticket.title(), ticket.description(), ticket.priority(), ticket.status(), ticket.createdAt());
    }
}
