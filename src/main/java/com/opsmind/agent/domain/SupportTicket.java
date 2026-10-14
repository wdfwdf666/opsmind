package com.opsmind.agent.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import java.time.Instant;
import java.util.UUID;

public record SupportTicket(
        UUID id /* 字段作用：对应对象的唯一标识。 */,
        UUID runId /* 字段作用：对应对象的唯一标识。 */,
        String title /* 字段作用：该不可变对象携带的 title 数据。 */,
        String description /* 字段作用：该不可变对象携带的 description 数据。 */,
        TicketPriority priority /* 字段作用：该不可变对象携带的 priority 数据。 */,
        TicketStatus status /* 字段作用：当前业务状态。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
