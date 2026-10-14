package com.opsmind.agent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsmind.agent.domain.AgentApproval;
import com.opsmind.agent.domain.AgentApprovalStatus;
import com.opsmind.agent.domain.AgentAuditRepository;
import com.opsmind.agent.domain.AgentException;
import com.opsmind.agent.domain.AgentRun;
import com.opsmind.agent.domain.AgentRunStatus;
import com.opsmind.agent.domain.AgentRunView;
import com.opsmind.agent.domain.AgentStep;
import com.opsmind.agent.domain.AgentStepType;
import com.opsmind.agent.domain.AgentTaskType;
import com.opsmind.agent.domain.SupportTicket;
import com.opsmind.agent.domain.SupportTicketRepository;
import com.opsmind.agent.domain.TicketDraft;
import com.opsmind.agent.domain.TicketPriority;
import com.opsmind.rag.application.RagAnswerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Agent 的核心状态机和用例编排器。
 * <p>一次运行会经过：接收请求 → Planner 路由 → 调用只读知识工具 → 返回答案，
 * 或生成工单草稿 → 等待人工审批 → 创建工单。每一步都写入审计表。</p>
 */
@Service
public class AgentRunService {

    private static final int MAX_QUERY_CHARACTERS = 1000;
    private static final int MAX_DECISION_REASON_CHARACTERS = 500;
    private static final int MAX_TICKET_TITLE_CHARACTERS = 180;
    private static final int MAX_TICKET_DESCRIPTION_CHARACTERS = 6000;

    private final AgentAuditRepository auditRepository;
    private final SupportTicketRepository ticketRepository;
    private final AgentTool<AgentToolInput, RagAnswerResult> knowledgeSearchTool;
    private final AgentTool<CreateTicketInput, SupportTicket> createTicketTool;
    private final AgentToolPolicy toolPolicy;
    private final AgentPlanner planner;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;
    private final ObjectMapper objectMapper;

    @Autowired
    public AgentRunService(
            AgentAuditRepository auditRepository,
            SupportTicketRepository ticketRepository,
            KnowledgeSearchAgentTool knowledgeSearchTool,
            CreateTicketAgentTool createTicketTool,
            AgentToolPolicy toolPolicy,
            AgentPlanner planner) {
        this(
                auditRepository, ticketRepository, knowledgeSearchTool, createTicketTool, toolPolicy, planner,
                Clock.systemUTC(), UUID::randomUUID, new ObjectMapper());
    }

    AgentRunService(
            AgentAuditRepository auditRepository,
            SupportTicketRepository ticketRepository,
            AgentTool<AgentToolInput, RagAnswerResult> knowledgeSearchTool,
            AgentTool<CreateTicketInput, SupportTicket> createTicketTool,
            AgentToolPolicy toolPolicy,
            AgentPlanner planner,
            Clock clock,
            Supplier<UUID> idSupplier,
            ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.ticketRepository = ticketRepository;
        this.knowledgeSearchTool = knowledgeSearchTool;
        this.createTicketTool = createTicketTool;
        this.toolPolicy = toolPolicy;
        this.planner = planner;
        this.clock = clock;
        this.idSupplier = idSupplier;
        this.objectMapper = objectMapper;
    }

    public AgentRunView start(String rawQuery, UUID documentId) {
        return start(rawQuery, documentId, null);
    }

    /** taskType 为空时交给 Planner 判断，非空时尊重调用方显式选择。 */
    public AgentRunView start(String rawQuery, UUID documentId, AgentTaskType requestedTaskType) {
        String query = validateQuery(rawQuery);
        AgentPlan plan = requestedTaskType == null ? planner.plan(query) : AgentPlan.explicit(requestedTaskType);
        AgentTaskType taskType = plan.taskType();
        UUID runId = idSupplier.get();
        Instant now = clock.instant();
        // 先落库再执行工具：即使后续失败，也能通过 runId 查询失败位置和原因。
        auditRepository.create(new AgentRun(
                runId, query, documentId, taskType, plan.source(), plan.provider(), plan.model(), plan.reason(),
                AgentRunStatus.RUNNING,
                null, null, null, null, List.of(), null, null, now, now, null));
        append(runId, 1, AgentStepType.RECEIVED, null, true,
                Map.of(
                        "queryCharacters", query.length(),
                        "scope", documentId == null ? "ALL_DOCUMENTS" : "DOCUMENT",
                        "taskType", taskType.name(),
                        "taskTypeSource", plan.source().name()));

        // Java switch 表达式进入两条受控流程，不让模型任意拼装工具调用。
        return switch (taskType) {
            case KNOWLEDGE_QA -> startKnowledgeRun(runId, query, documentId, plan);
            case CREATE_TICKET -> startTicketRun(runId, query, documentId, plan);
        };
    }

    // @Transactional 表示审批、创建工单和状态更新共享数据库事务；异常时整体回滚。
    @Transactional
    public AgentRunView decide(UUID runId, AgentApprovalStatus decision, String rawReason) {
        if (decision == null || decision == AgentApprovalStatus.PENDING) {
            throw new AgentException(
                    "AGENT_APPROVAL_DECISION_INVALID",
                    "Approval decision must be APPROVED or REJECTED",
                    HttpStatus.BAD_REQUEST);
        }
        String reason = validateDecisionReason(rawReason);
        AgentRunView current = get(runId);
        if (current.run().taskType() != AgentTaskType.CREATE_TICKET || current.approval() == null) {
            throw new AgentException(
                    "AGENT_APPROVAL_NOT_APPLICABLE",
                    "Agent run does not contain a pending write tool",
                    HttpStatus.CONFLICT);
        }
        // 同一决定重复提交直接返回原结果，避免网络重试创建两张工单（幂等）。
        if (isSameCompletedDecision(current, decision)) {
            return current;
        }
        if (current.run().status() != AgentRunStatus.WAITING_APPROVAL
                || current.approval().status() != AgentApprovalStatus.PENDING) {
            throw new AgentException(
                    "AGENT_APPROVAL_ALREADY_DECIDED",
                    "Agent approval has already been decided",
                    HttpStatus.CONFLICT);
        }

        Instant decidedAt = clock.instant();
        // 条件更新只有一个并发请求能成功，其他请求得到冲突而不会重复执行写工具。
        if (!auditRepository.decideApproval(runId, decision, reason, decidedAt)) {
            throw new AgentException(
                    "AGENT_APPROVAL_CONCURRENT_UPDATE",
                    "Agent approval was changed by another request",
                    HttpStatus.CONFLICT);
        }

        if (decision == AgentApprovalStatus.REJECTED) {
            append(runId, 5, AgentStepType.REJECTED, createTicketTool, true,
                    Map.of("reasonProvided", reason != null));
            auditRepository.reject(runId, "工单创建已被审批人拒绝。", decidedAt);
            return get(runId);
        }

        toolPolicy.requireApprovedExecutable(createTicketTool);
        append(runId, 5, AgentStepType.APPROVED, createTicketTool, true,
                Map.of("reasonProvided", reason != null));
        // 只有 APPROVED 分支且策略校验通过后，真正的写工具才会运行。
        SupportTicket ticket = createTicketTool.execute(new CreateTicketInput(runId, current.approval().draft()));
        append(runId, 6, AgentStepType.TOOL_CALL, createTicketTool, true,
                Map.of("ticketId", ticket.id(), "ticketStatus", ticket.status().name()));
        String answer = "工单已创建，编号：" + ticket.id();
        append(runId, 7, AgentStepType.RESPONDED, null, true,
                Map.of("answerCharacters", answer.length()));
        auditRepository.completeWrite(runId, answer, clock.instant());
        return get(runId);
    }

    public AgentRunView get(UUID runId) {
        AgentRun run = auditRepository.findRun(runId)
                .orElseThrow(() -> new AgentException(
                        "AGENT_RUN_NOT_FOUND", "Agent run not found: " + runId, HttpStatus.NOT_FOUND));
        return new AgentRunView(
                run,
                auditRepository.findSteps(runId),
                auditRepository.findApproval(runId).orElse(null),
                ticketRepository.findByRunId(runId).orElse(null));
    }

    private AgentRunView startKnowledgeRun(UUID runId, String query, UUID documentId, AgentPlan plan) {
        append(runId, 2, AgentStepType.PLANNED, knowledgeSearchTool, true,
                Map.of("strategy", "CONTROLLED_SINGLE_TOOL", "tool", knowledgeSearchTool.name(),
                        "plannerProvider", plan.provider(), "plannerModel", plan.model(), "plannerReason", plan.reason()));
        RagAnswerResult answer = executeKnowledge(runId, query, documentId, 3);
        append(runId, 4, AgentStepType.RESPONDED, null, true,
                Map.of("answerCharacters", answer.answer().length(), "totalTokens", value(answer.totalTokens())));
        auditRepository.complete(runId, answer, clock.instant());
        return get(runId);
    }

    private AgentRunView startTicketRun(UUID runId, String query, UUID documentId, AgentPlan plan) {
        append(runId, 2, AgentStepType.PLANNED, null, true,
                Map.of(
                        "strategy", "DETERMINISTIC_APPROVAL_FLOW",
                        "readTool", knowledgeSearchTool.name(),
                        "writeTool", createTicketTool.name(),
                        "plannerProvider", plan.provider(),
                        "plannerModel", plan.model(),
                        "plannerReason", plan.reason()));
        // 创建工单前仍先检索知识，生成带建议的草稿；此时还没有任何外部写操作。
        RagAnswerResult evidence = executeKnowledge(runId, query, documentId, 3);
        TicketDraft draft = ticketDraft(query, evidence);
        Instant requestedAt = clock.instant();
        AgentApproval approval = new AgentApproval(
                idSupplier.get(), runId, createTicketTool.name(), createTicketTool.risk(),
                AgentApprovalStatus.PENDING, draft, null, requestedAt, null);
        AgentStep approvalStep = step(runId, 4, AgentStepType.APPROVAL_REQUESTED, createTicketTool, true,
                Map.of(
                        "approvalId", approval.id(),
                        "priority", draft.priority().name(),
                        "titleCharacters", draft.title().length(),
                        "descriptionCharacters", draft.description().length()));
        auditRepository.requestApproval(runId, evidence, approval, approvalStep, requestedAt);
        return get(runId);
    }

    private RagAnswerResult executeKnowledge(UUID runId, String query, UUID documentId, int sequence) {
        try {
            toolPolicy.requireAutoExecutable(knowledgeSearchTool);
            RagAnswerResult answer = knowledgeSearchTool.execute(new AgentToolInput(query, documentId));
            append(runId, sequence, AgentStepType.TOOL_CALL, knowledgeSearchTool, true,
                    Map.of(
                            "refused", answer.refused(),
                            "refusalReason", answer.refusalReason() == null ? "" : answer.refusalReason().name(),
                            "citationCount", answer.citations().size(),
                            "topEvidenceScore", answer.topEvidenceScore() == null ? -1 : answer.topEvidenceScore()));
            return answer;
        } catch (RuntimeException exception) {
            String code = exception instanceof AgentException agentException
                    ? agentException.code()
                    : "AGENT_TOOL_EXECUTION_FAILED";
            append(runId, sequence, AgentStepType.TOOL_CALL, knowledgeSearchTool, false,
                    Map.of("errorCode", code));
            auditRepository.fail(runId, code, "Agent execution failed", clock.instant());
            throw new AgentException(
                    "AGENT_RUN_FAILED", "Agent run failed; runId=" + runId, HttpStatus.BAD_GATEWAY);
        }
    }

    private boolean isSameCompletedDecision(AgentRunView view, AgentApprovalStatus decision) {
        return (view.run().status() == AgentRunStatus.COMPLETED
                && view.approval().status() == AgentApprovalStatus.APPROVED
                && decision == AgentApprovalStatus.APPROVED)
                || (view.run().status() == AgentRunStatus.REJECTED
                && view.approval().status() == AgentApprovalStatus.REJECTED
                && decision == AgentApprovalStatus.REJECTED);
    }

    private TicketDraft ticketDraft(String query, RagAnswerResult evidence) {
        String title = truncate(query.replaceAll("\\s+", " "), MAX_TICKET_TITLE_CHARACTERS);
        String description = "用户描述：\n" + query + "\n\n知识库建议：\n" + evidence.answer();
        return new TicketDraft(title, truncate(description, MAX_TICKET_DESCRIPTION_CHARACTERS), TicketPriority.P2);
    }

    private void append(
            UUID runId,
            int sequence,
            AgentStepType type,
            AgentTool<?, ?> tool,
            boolean success,
            Map<String, ?> detail) {
        // 所有轨迹统一从这里追加，sequence 保证 UI 按真实执行顺序展示。
        auditRepository.appendStep(step(runId, sequence, type, tool, success, detail));
    }

    private AgentStep step(
            UUID runId,
            int sequence,
            AgentStepType type,
            AgentTool<?, ?> tool,
            boolean success,
            Map<String, ?> detail) {
        return new AgentStep(
                idSupplier.get(), runId, sequence, type,
                tool == null ? null : tool.name(), tool == null ? null : tool.risk(),
                success, json(detail), clock.instant());
    }

    private String validateQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new AgentException("AGENT_QUERY_REQUIRED", "Agent query must not be blank", HttpStatus.BAD_REQUEST);
        }
        String query = rawQuery.strip();
        if (query.length() > MAX_QUERY_CHARACTERS) {
            throw new AgentException(
                    "AGENT_QUERY_TOO_LONG",
                    "Agent query must not exceed " + MAX_QUERY_CHARACTERS + " characters",
                    HttpStatus.BAD_REQUEST);
        }
        return query;
    }

    private String validateDecisionReason(String rawReason) {
        if (rawReason == null || rawReason.isBlank()) {
            return null;
        }
        String reason = rawReason.strip();
        if (reason.length() > MAX_DECISION_REASON_CHARACTERS) {
            throw new AgentException(
                    "AGENT_APPROVAL_REASON_TOO_LONG",
                    "Approval reason must not exceed " + MAX_DECISION_REASON_CHARACTERS + " characters",
                    HttpStatus.BAD_REQUEST);
        }
        return reason;
    }

    private String truncate(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private String json(Map<String, ?> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize agent audit detail", exception);
        }
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
