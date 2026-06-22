package com.example.aiticket.ticket.service;

import com.example.aiticket.ai.rag.domain.AiMessage;
import com.example.aiticket.ai.rag.domain.AiSession;
import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketComment;
import com.example.aiticket.ticket.domain.TicketCommentType;
import com.example.aiticket.ticket.domain.TicketDetail;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;
import com.example.aiticket.ticket.domain.TicketWorkflowAction;
import com.example.aiticket.ticket.mapper.TicketMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TicketWorkflowService {
    private static final DateTimeFormatter TICKET_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TicketMapper mapper;
    private final AssignmentStrategy assignmentStrategy;
    private final SlaPolicy slaPolicy;

    public TicketWorkflowService(TicketMapper mapper, AssignmentStrategy assignmentStrategy) {
        this(mapper, assignmentStrategy, new SlaPolicy());
    }

    @Autowired
    public TicketWorkflowService(TicketMapper mapper, AssignmentStrategy assignmentStrategy, SlaPolicy slaPolicy) {
        this.mapper = mapper;
        this.assignmentStrategy = assignmentStrategy;
        this.slaPolicy = slaPolicy;
    }

    @Transactional
    public Ticket createFromAiSession(Long userId, String operatorRole, CreateTicketFromAiSessionCommand command) {
        if (command == null || command.sessionId() == null) {
            throw new TicketWorkflowException("AI session not found");
        }
        AiSession session = mapper.findOwnedAiSession(command.sessionId(), userId);
        if (session == null) {
            throw new TicketWorkflowException("AI session not found");
        }

        AiMessage assistantMessage = resolveAssistantMessage(userId, command);
        Long ticketId = mapper.nextTicketId();
        String ticketNo = ticketNo(ticketId);
        String title = normalized(command.title(), session.title());
        String description = normalized(command.description(), session.lastQuestion());
        TicketPriority priority = command.priority() == null ? TicketPriority.NORMAL : command.priority();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadlineAt = slaPolicy.deadlineFor(priority, now);
        String transferReason = normalized(command.transferReason(),
                assistantMessage == null ? "用户请求人工处理" : assistantMessage.transferReason());
        String aiSuggestion = assistantMessage == null ? null : assistantMessage.content();

        mapper.insertTicket(
                ticketId,
                ticketNo,
                title,
                description,
                TicketStatus.PENDING_ASSIGN,
                priority,
                null,
                command.categoryId(),
                null,
                userId,
                null,
                TicketSource.AI_SESSION,
                session.id(),
                assistantMessage == null ? null : assistantMessage.id(),
                title,
                aiSuggestion,
                transferReason,
                deadlineAt
        );
        insertFlow(ticketId, null, TicketStatus.PENDING_ASSIGN, TicketWorkflowAction.CREATE,
                userId, operatorRole, transferReason);

        return new Ticket(ticketId, ticketNo, title, description, TicketStatus.PENDING_ASSIGN, priority,
                null, command.categoryId(), null, userId, null, TicketSource.AI_SESSION,
                session.id(), assistantMessage == null ? null : assistantMessage.id(), title, aiSuggestion,
                transferReason, deadlineAt, null, null, 0, false, now, now);
    }

    @Transactional
    public Ticket assign(Long operatorId, String operatorRole, Long ticketId, Long assigneeId, String comment) {
        Ticket current = mapper.findTicketForUpdate(ticketId);
        if (current == null) {
            throw new TicketNotFoundException("ticket not found");
        }
        if (current.status() != TicketStatus.PENDING_ASSIGN) {
            throw new TicketWorkflowException("ticket status " + current.status() + " does not allow ASSIGN");
        }

        Long selectedAssigneeId = assignmentStrategy.selectAssignee(
                new TicketAssignmentContext(current, operatorId, operatorRole, assigneeId, comment));

        mapper.updateTicketStatus(ticketId, TicketStatus.PENDING_PROCESS, selectedAssigneeId, null, null, 0);
        insertFlow(ticketId, current.status(), TicketStatus.PENDING_PROCESS, TicketWorkflowAction.ASSIGN,
                operatorId, operatorRole, comment);
        return copy(current, TicketStatus.PENDING_PROCESS, selectedAssigneeId, null, null, 0);
    }

    public List<Ticket> listCreatedTickets(Long creatorId, int limit) {
        return mapper.listCreatedTickets(creatorId, normalizedLimit(limit));
    }

    public List<Ticket> listAssignedTickets(Long assigneeId, int limit) {
        return mapper.listAssignedTickets(assigneeId, normalizedLimit(limit));
    }

    public List<Ticket> listManagedTickets(int limit) {
        return mapper.listManagedTickets(normalizedLimit(limit));
    }

    public Ticket getTicket(Long userId, Long ticketId, boolean canManage, boolean canProcess) {
        Ticket ticket;
        if (canManage) {
            ticket = mapper.findTicketById(ticketId);
        } else if (canProcess) {
            ticket = mapper.findAssignedTicket(ticketId, userId);
            if (ticket == null) {
                ticket = mapper.findCreatedTicket(ticketId, userId);
            }
        } else {
            ticket = mapper.findCreatedTicket(ticketId, userId);
        }
        if (ticket == null) {
            throw new TicketNotFoundException("ticket not found");
        }
        return ticket;
    }

    public TicketDetail getTicketDetail(Long userId, Long ticketId, boolean canManage, boolean canProcess) {
        Ticket ticket = getTicket(userId, ticketId, canManage, canProcess);
        return new TicketDetail(ticket, mapper.listFlowLogs(ticket.id()));
    }

    @Transactional
    public TicketComment addComment(Long userId, String operatorRole, Long ticketId, boolean canManage,
                                    boolean canProcess, AddTicketCommentCommand command) {
        Ticket ticket = getTicket(userId, ticketId, canManage, canProcess);
        TicketCommentType type = normalizedCommentType(command);
        String content = normalizedContent(command);
        requireCommentPermission(ticket, userId, canManage, canProcess, type);

        Long commentId = mapper.nextCommentId();
        boolean internal = type == TicketCommentType.INTERNAL_NOTE || type == TicketCommentType.SYSTEM;
        mapper.insertComment(commentId, ticket.id(), userId, type, content, internal);
        return new TicketComment(commentId, ticket.id(), userId, type, content, internal, LocalDateTime.now());
    }

    public List<TicketComment> listComments(Long userId, Long ticketId, boolean canManage, boolean canProcess) {
        Ticket ticket = getTicket(userId, ticketId, canManage, canProcess);
        return mapper.listComments(ticket.id(), canSeeInternalComments(ticket, userId, canManage, canProcess));
    }

    @Transactional
    public Ticket startProcessing(Long operatorId, String operatorRole, Long ticketId, String comment) {
        Ticket current = lockedTicket(ticketId);
        requireAssignee(current, operatorId);
        requireStatus(current, TicketWorkflowAction.START_PROCESS, TicketStatus.PENDING_PROCESS);
        mapper.updateTicketStatus(ticketId, TicketStatus.PROCESSING, current.assigneeId(), null, null, 0);
        insertFlow(ticketId, current.status(), TicketStatus.PROCESSING, TicketWorkflowAction.START_PROCESS,
                operatorId, operatorRole, comment);
        return copy(current, TicketStatus.PROCESSING, current.assigneeId(), null, null, 0);
    }

    @Transactional
    public Ticket resolve(Long operatorId, String operatorRole, Long ticketId, String comment) {
        Ticket current = lockedTicket(ticketId);
        requireAssignee(current, operatorId);
        requireStatus(current, TicketWorkflowAction.RESOLVE, TicketStatus.PROCESSING);
        LocalDateTime now = LocalDateTime.now();
        mapper.updateTicketStatus(ticketId, TicketStatus.RESOLVED, current.assigneeId(), now, null, 0);
        insertFlow(ticketId, current.status(), TicketStatus.RESOLVED, TicketWorkflowAction.RESOLVE,
                operatorId, operatorRole, comment);
        return copy(current, TicketStatus.RESOLVED, current.assigneeId(), now, null, 0);
    }

    @Transactional
    public Ticket reopen(Long operatorId, String operatorRole, Long ticketId, String comment) {
        Ticket current = lockedTicket(ticketId);
        requireCreator(current, operatorId);
        requireStatus(current, TicketWorkflowAction.REOPEN, TicketStatus.RESOLVED);
        mapper.updateTicketStatus(ticketId, TicketStatus.PROCESSING, current.assigneeId(), null, null, 1);
        insertFlow(ticketId, current.status(), TicketStatus.PROCESSING, TicketWorkflowAction.REOPEN,
                operatorId, operatorRole, comment);
        return copy(current, TicketStatus.PROCESSING, current.assigneeId(), null, null, 1);
    }

    @Transactional
    public Ticket confirmClose(Long operatorId, String operatorRole, Long ticketId, String comment) {
        Ticket current = lockedTicket(ticketId);
        requireCreator(current, operatorId);
        requireStatus(current, TicketWorkflowAction.CONFIRM_CLOSE, TicketStatus.RESOLVED);
        LocalDateTime now = LocalDateTime.now();
        mapper.updateTicketStatus(ticketId, TicketStatus.CLOSED, current.assigneeId(), null, now, 0);
        insertFlow(ticketId, current.status(), TicketStatus.CLOSED, TicketWorkflowAction.CONFIRM_CLOSE,
                operatorId, operatorRole, comment);
        return copy(current, TicketStatus.CLOSED, current.assigneeId(), null, now, 0);
    }

    @Transactional
    public Ticket close(Long operatorId, String operatorRole, Long ticketId, String comment) {
        Ticket current = lockedTicket(ticketId);
        if (current.status() == TicketStatus.CLOSED) {
            throw new TicketWorkflowException("ticket status CLOSED does not allow CLOSE");
        }
        LocalDateTime now = LocalDateTime.now();
        mapper.updateTicketStatus(ticketId, TicketStatus.CLOSED, current.assigneeId(), null, now, 0);
        insertFlow(ticketId, current.status(), TicketStatus.CLOSED, TicketWorkflowAction.CLOSE,
                operatorId, operatorRole, comment);
        return copy(current, TicketStatus.CLOSED, current.assigneeId(), null, now, 0);
    }

    private AiMessage resolveAssistantMessage(Long userId, CreateTicketFromAiSessionCommand command) {
        if (command.assistantMessageId() == null) {
            return mapper.findLatestAssistantMessage(command.sessionId(), userId);
        }
        AiMessage message = mapper.findOwnedAssistantMessage(command.assistantMessageId(), command.sessionId(), userId);
        if (message == null) {
            throw new TicketWorkflowException("AI assistant message not found");
        }
        return message;
    }

    private void insertFlow(Long ticketId, TicketStatus fromStatus, TicketStatus toStatus,
                            TicketWorkflowAction action, Long operatorId, String operatorRole, String commentText) {
        mapper.insertFlowLog(mapper.nextFlowLogId(), ticketId, fromStatus, toStatus, action,
                operatorId, operatorRole, commentText);
    }

    private Ticket lockedTicket(Long ticketId) {
        Ticket ticket = mapper.findTicketForUpdate(ticketId);
        if (ticket == null) {
            throw new TicketNotFoundException("ticket not found");
        }
        return ticket;
    }

    private void requireAssignee(Ticket ticket, Long operatorId) {
        if (ticket.assigneeId() == null || !ticket.assigneeId().equals(operatorId)) {
            throw new TicketNotFoundException("ticket not found");
        }
    }

    private void requireCreator(Ticket ticket, Long operatorId) {
        if (!ticket.creatorId().equals(operatorId)) {
            throw new TicketNotFoundException("ticket not found");
        }
    }

    private void requireStatus(Ticket ticket, TicketWorkflowAction action, TicketStatus expected) {
        if (ticket.status() != expected) {
            throw new TicketWorkflowException("ticket status " + ticket.status() + " does not allow " + action);
        }
    }

    private TicketCommentType normalizedCommentType(AddTicketCommentCommand command) {
        if (command == null || command.commentType() == null) {
            throw new TicketWorkflowException("comment type is required");
        }
        if (command.commentType() == TicketCommentType.SYSTEM) {
            throw new TicketWorkflowException("comment type SYSTEM is not allowed");
        }
        return command.commentType();
    }

    private String normalizedContent(AddTicketCommentCommand command) {
        if (command == null || command.content() == null || command.content().isBlank()) {
            throw new TicketWorkflowException("comment content is required");
        }
        return command.content().trim();
    }

    private void requireCommentPermission(Ticket ticket, Long userId, boolean canManage,
                                          boolean canProcess, TicketCommentType type) {
        boolean creator = ticket.creatorId().equals(userId);
        boolean assignedProcessor = canProcess && userId.equals(ticket.assigneeId());
        if (type == TicketCommentType.USER_REPLY && creator) {
            return;
        }
        if ((type == TicketCommentType.AGENT_REPLY || type == TicketCommentType.INTERNAL_NOTE)
                && (canManage || assignedProcessor)) {
            return;
        }
        throw new TicketWorkflowException("comment type " + type + " is not allowed");
    }

    private boolean canSeeInternalComments(Ticket ticket, Long userId, boolean canManage, boolean canProcess) {
        return canManage || (canProcess && userId.equals(ticket.assigneeId()));
    }

    private int normalizedLimit(int limit) {
        if (limit <= 0) {
            return 100;
        }
        return Math.min(limit, 200);
    }

    private String ticketNo(Long ticketId) {
        return "TK" + LocalDateTime.now().format(TICKET_NO_TIME) + String.format("%06d", ticketId);
    }

    private String normalized(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return "未命名工单";
    }

    private Ticket copy(Ticket ticket, TicketStatus status, Long assigneeId,
                        LocalDateTime firstResolvedAt, LocalDateTime closedAt, int reopenDelta) {
        int reopenCount = ticket.reopenCount() == null ? 0 : ticket.reopenCount();
        return new Ticket(ticket.id(), ticket.ticketNo(), ticket.title(), ticket.description(), status,
                ticket.priority(), ticket.aiPrioritySuggestion(), ticket.categoryId(), ticket.departmentId(),
                ticket.creatorId(), assigneeId, ticket.source(), ticket.sourceSessionId(), ticket.sourceMessageId(),
                ticket.aiSummary(), ticket.aiSuggestion(), ticket.transferReason(), ticket.deadlineAt(),
                firstResolvedAt == null ? ticket.firstResolvedAt() : firstResolvedAt,
                closedAt == null ? ticket.closedAt() : closedAt, reopenCount + reopenDelta, ticket.deleted(),
                ticket.createdAt(), LocalDateTime.now());
    }
}
