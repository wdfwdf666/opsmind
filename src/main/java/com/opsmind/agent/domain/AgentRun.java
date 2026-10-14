package com.opsmind.agent.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import com.opsmind.rag.application.RagCitation;
import com.opsmind.rag.application.RagRefusalReason;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentRun(
        UUID id /* 字段作用：对应对象的唯一标识。 */,
        String query /* 字段作用：用户输入的问题或检索文本。 */,
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        AgentTaskType taskType /* 字段作用：该不可变对象携带的 taskType 数据。 */,
        AgentTaskTypeSource taskTypeSource /* 字段作用：该不可变对象携带的 taskTypeSource 数据。 */,
        String plannerProvider /* 字段作用：当前选择的能力提供方式，例如 qwen 或 local。 */,
        String plannerModel /* 字段作用：该不可变对象携带的 plannerModel 数据。 */,
        String plannerReason /* 字段作用：产生当前状态或决策的原因。 */,
        AgentRunStatus status /* 字段作用：当前业务状态。 */,
        String answer /* 字段作用：该不可变对象携带的 answer 数据。 */,
        Boolean refused /* 字段作用：该不可变对象携带的 refused 数据。 */,
        RagRefusalReason refusalReason /* 字段作用：产生当前状态或决策的原因。 */,
        Double topEvidenceScore /* 字段作用：相关性或质量分数，含义由当前算法定义。 */,
        List<RagCitation> citations /* 字段作用：该不可变对象携带的 citations 数据。 */,
        String errorCode /* 字段作用：该不可变对象携带的 errorCode 数据。 */,
        String errorMessage /* 字段作用：该不可变对象携带的 errorMessage 数据。 */,
        Instant createdAt /* 字段作用：对象创建时间。 */,
        Instant updatedAt /* 字段作用：最近更新或完成时间。 */,
        Instant completedAt /* 字段作用：最近更新或完成时间。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
