package com.opsmind.agent.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

import com.opsmind.rag.application.RagAnswerResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentAuditRepository { // 类型作用：仓储接口，业务层依赖它而不是依赖具体 SQL。

    void create(AgentRun run); // 接口约定：规定实现类必须创建或保存数据，并返回持久化结果。

    void appendStep(AgentStep step); // 接口约定：规定实现类必须完成“appendStep”对应的内部步骤，供主流程复用。

    void complete(UUID runId, RagAnswerResult answer, Instant completedAt); // 接口约定：规定实现类必须完成“complete”对应的内部步骤，供主流程复用。

    void requestApproval(
            UUID runId,
            RagAnswerResult evidence,
            AgentApproval approval,
            AgentStep approvalStep,
            Instant updatedAt); // 接口约定：规定实现类必须完成“requestApproval”对应的内部步骤，供主流程复用。

    boolean decideApproval(
            UUID runId,
            AgentApprovalStatus decision,
            String decisionReason,
            Instant decidedAt); // 接口约定：规定实现类必须完成“decideApproval”对应的内部步骤，供主流程复用。

    void completeWrite(UUID runId, String answer, Instant completedAt); // 接口约定：规定实现类必须完成“completeWrite”对应的内部步骤，供主流程复用。

    void reject(UUID runId, String answer, Instant completedAt); // 接口约定：规定实现类必须完成“reject”对应的内部步骤，供主流程复用。

    void fail(UUID runId, String errorCode, String errorMessage, Instant completedAt); // 接口约定：规定实现类必须完成“fail”对应的内部步骤，供主流程复用。

    Optional<AgentRun> findRun(UUID runId); // 接口约定：规定实现类必须按条件读取目标数据，不存在时按约定返回空值或异常。

    Optional<AgentApproval> findApproval(UUID runId); // 接口约定：规定实现类必须按条件读取目标数据，不存在时按约定返回空值或异常。

    List<AgentStep> findSteps(UUID runId); // 接口约定：规定实现类必须按条件读取目标数据，不存在时按约定返回空值或异常。
}
