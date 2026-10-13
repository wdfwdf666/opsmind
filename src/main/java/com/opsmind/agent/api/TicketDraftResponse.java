package com.opsmind.agent.api;

import com.opsmind.agent.domain.TicketDraft;
import com.opsmind.agent.domain.TicketPriority;

public record TicketDraftResponse(
        String title,
        String description,
        TicketPriority priority) {

    static TicketDraftResponse from(TicketDraft draft) {
        return new TicketDraftResponse(draft.title(), draft.description(), draft.priority());
    }
}
