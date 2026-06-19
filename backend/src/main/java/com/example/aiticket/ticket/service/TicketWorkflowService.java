package com.example.aiticket.ticket.service;

import com.example.aiticket.ai.rag.domain.AiMessage;
import com.example.aiticket.ai.rag.domain.AiSession;
import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;
import com.example.aiticket.ticket.domain.TicketWorkflowAction;
import com.example.aiticket.ticket.mapper.TicketMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TicketWorkflowService {
    private static final DateTimeFormatter TICKET_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TicketMapper mapper;

    public TicketWorkflowService(TicketMapper mapper) {
        this.mapper = mapper;
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
                null
        );
        insertFlow(ticketId, null, TicketStatus.PENDING_ASSIGN, TicketWorkflowAction.CREATE,
                userId, operatorRole, transferReason);

        return new Ticket(ticketId, ticketNo, title, description, TicketStatus.PENDING_ASSIGN, priority,
                null, command.categoryId(), null, userId, null, TicketSource.AI_SESSION,
                session.id(), assistantMessage == null ? null : assistantMessage.id(), title, aiSuggestion,
                transferReason, null, null, null, 0, false, LocalDateTime.now(), LocalDateTime.now());
    }

    @Transactional
    public Ticket assign(Long operatorId, String operatorRole, Long ticketId, Long assigneeId, String comment) {
        if (assigneeId == null) {
            throw new TicketWorkflowException("assignee is required");
        }
        Ticket current = mapper.findTicketForUpdate(ticketId);
        if (current == null) {
            throw new TicketNotFoundException("ticket not found");
        }
        if (current.status() != TicketStatus.PENDING_ASSIGN) {
            throw new TicketWorkflowException("ticket status " + current.status() + " does not allow ASSIGN");
        }

        mapper.updateTicketStatus(ticketId, TicketStatus.PENDING_PROCESS, assigneeId, null, null, 0);
        insertFlow(ticketId, current.status(), TicketStatus.PENDING_PROCESS, TicketWorkflowAction.ASSIGN,
                operatorId, operatorRole, comment);
        return copy(current, TicketStatus.PENDING_PROCESS, assigneeId, null, null, 0);
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
        return new Ticket(ticket.id(), ticket.ticketNo(), ticket.title(), ticket.description(), status,
                ticket.priority(), ticket.aiPrioritySuggestion(), ticket.categoryId(), ticket.departmentId(),
                ticket.creatorId(), assigneeId, ticket.source(), ticket.sourceSessionId(), ticket.sourceMessageId(),
                ticket.aiSummary(), ticket.aiSuggestion(), ticket.transferReason(), ticket.deadlineAt(),
                firstResolvedAt == null ? ticket.firstResolvedAt() : firstResolvedAt,
                closedAt, ticket.reopenCount() + reopenDelta, ticket.deleted(),
                ticket.createdAt(), LocalDateTime.now());
    }
}
