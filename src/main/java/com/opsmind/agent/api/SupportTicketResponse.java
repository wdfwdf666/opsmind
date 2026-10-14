package com.opsmind.agent.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.agent.domain.SupportTicket;
import com.opsmind.agent.domain.TicketPriority;
import com.opsmind.agent.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record SupportTicketResponse(
        UUID ticketId /* 字段作用：对应对象的唯一标识。 */,
        String title /* 字段作用：该不可变对象携带的 title 数据。 */,
        String description /* 字段作用：该不可变对象携带的 description 数据。 */,
        TicketPriority priority /* 字段作用：该不可变对象携带的 priority 数据。 */,
        TicketStatus status /* 字段作用：当前业务状态。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static SupportTicketResponse from(SupportTicket ticket) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        if (ticket == null) {
            return null;
        }
        return new SupportTicketResponse(
                ticket.id(), ticket.title(), ticket.description(), ticket.priority(), ticket.status(), ticket.createdAt());
    }
}
