package com.opsmind.agent.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.util.Optional;
import java.util.UUID;

public interface SupportTicketRepository {
    // 类型作用：仓储接口，业务层依赖它而不是依赖具体 SQL。

    SupportTicket create(SupportTicket ticket);
    // 接口约定：规定实现类必须创建或保存数据，并返回持久化结果。

    Optional<SupportTicket> findByRunId(UUID runId);
    // 接口约定：规定实现类必须按条件读取目标数据，不存在时按约定返回空值或异常。
}
