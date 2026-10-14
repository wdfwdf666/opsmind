package com.opsmind.agent.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.agent.domain.AgentToolRisk;
import com.opsmind.agent.domain.SupportTicket;
import com.opsmind.agent.domain.SupportTicketRepository;
import com.opsmind.agent.domain.TicketStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

@Component // 注解作用：把普通组件交给 Spring 容器管理。
public class CreateTicketAgentTool implements AgentTool<CreateTicketInput, SupportTicket> { // 类型作用：Agent 工具，把一项能力封装成统一名称、风险级别和执行入口。

    private final SupportTicketRepository ticketRepository;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;

    @Autowired // 注解作用：要求 Spring 按构造器参数类型注入依赖。
    public CreateTicketAgentTool(SupportTicketRepository ticketRepository) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this(ticketRepository, Clock.systemUTC(), UUID::randomUUID);
    }

    CreateTicketAgentTool(
            SupportTicketRepository ticketRepository,
            Clock clock,
            Supplier<UUID> idSupplier) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.ticketRepository = ticketRepository;
        this.clock = clock;
        this.idSupplier = idSupplier;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public String name() { // 方法作用：完成“name”对应的内部步骤，供主流程复用。
        return "create_ticket";
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public AgentToolRisk risk() { // 方法作用：完成“risk”对应的内部步骤，供主流程复用。
        return AgentToolRisk.WRITE_REQUIRES_APPROVAL;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public SupportTicket execute(CreateTicketInput input) { // 方法作用：执行当前工具封装的具体能力并返回结果。
        return ticketRepository.create(new SupportTicket(
                idSupplier.get(), input.runId(), input.draft().title(), input.draft().description(),
                input.draft().priority(), TicketStatus.OPEN, clock.instant()));
    }
}
