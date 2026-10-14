package com.opsmind.agent.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.agent.domain.AgentTaskType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record AgentRunRequest(
        @Schema(example = "数据库连接池告警应该检查什么？") String query /* 字段作用：用户输入的问题或检索文本。 */,
        @Schema(
                format = "uuid",
                nullable = true,
                description = "Optional debug scope. Omit it to let RAG select documents automatically.")
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        @Schema(
                nullable = true,
                description = "Optional debug override. Omit it to let the configured Planner select an allowed task type.",
                example = "KNOWLEDGE_QA")
        AgentTaskType taskType /* 字段作用：该不可变对象携带的 taskType 数据。 */) { // 类型作用：请求 DTO，规定调用接口时可以提交哪些 JSON 字段。
}
