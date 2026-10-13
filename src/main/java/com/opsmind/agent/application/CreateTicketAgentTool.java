package com.opsmind.agent.application;

import com.opsmind.agent.domain.AgentToolRisk;
import com.opsmind.agent.domain.SupportTicket;
import com.opsmind.agent.domain.SupportTicketRepository;
import com.opsmind.agent.domain.TicketStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class CreateTicketAgentTool implements AgentTool<CreateTicketInput, SupportTicket> {

    private final SupportTicketRepository ticketRepository;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;

    @Autowired
    public CreateTicketAgentTool(SupportTicketRepository ticketRepository) {
        this(ticketRepository, Clock.systemUTC(), UUID::randomUUID);
    }

    CreateTicketAgentTool(
            SupportTicketRepository ticketRepository,
            Clock clock,
            Supplier<UUID> idSupplier) {
        this.ticketRepository = ticketRepository;
        this.clock = clock;
        this.idSupplier = idSupplier;
    }

    @Override
    public String name() {
        return "create_ticket";
    }

    @Override
    public AgentToolRisk risk() {
        return AgentToolRisk.WRITE_REQUIRES_APPROVAL;
    }

    @Override
    public SupportTicket execute(CreateTicketInput input) {
        return ticketRepository.create(new SupportTicket(
                idSupplier.get(), input.runId(), input.draft().title(), input.draft().description(),
                input.draft().priority(), TicketStatus.OPEN, clock.instant()));
    }
}
