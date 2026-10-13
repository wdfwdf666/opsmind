package com.opsmind.agent.application;

import com.opsmind.agent.domain.TicketDraft;

import java.util.UUID;

public record CreateTicketInput(UUID runId, TicketDraft draft) {
}
