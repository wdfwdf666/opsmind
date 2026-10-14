package com.opsmind.agent.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.agent.domain.TicketDraft;
import com.opsmind.agent.domain.TicketPriority;

public record TicketDraftResponse(
        String title /* 字段作用：该不可变对象携带的 title 数据。 */,
        String description /* 字段作用：该不可变对象携带的 description 数据。 */,
        TicketPriority priority /* 字段作用：该不可变对象携带的 priority 数据。 */) { // 类型作用：响应 DTO，规定接口返回给前端的 JSON 字段。

    static TicketDraftResponse from(TicketDraft draft) { // 方法作用：把内部领域对象转换为对外响应 DTO。
        return new TicketDraftResponse(draft.title(), draft.description(), draft.priority());
    }
}
