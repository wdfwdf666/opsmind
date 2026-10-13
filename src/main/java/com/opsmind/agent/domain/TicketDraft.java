package com.opsmind.agent.domain;

public record TicketDraft(
        String title,
        String description,
        TicketPriority priority) {
}
