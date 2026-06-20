package com.example.aiticket.ticket.mapper;

import com.example.aiticket.ai.rag.domain.AiMessage;
import com.example.aiticket.ai.rag.domain.AiSession;
import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketComment;
import com.example.aiticket.ticket.domain.TicketCommentType;
import com.example.aiticket.ticket.domain.TicketFlowLog;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;
import com.example.aiticket.ticket.domain.TicketWorkflowAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TicketMapper {
    Long nextTicketId();

    Long nextFlowLogId();

    Long nextCommentId();

    AiSession findOwnedAiSession(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    AiMessage findLatestAssistantMessage(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    AiMessage findOwnedAssistantMessage(@Param("messageId") Long messageId,
                                         @Param("sessionId") Long sessionId,
                                         @Param("userId") Long userId);

    int insertTicket(@Param("id") Long id,
                     @Param("ticketNo") String ticketNo,
                     @Param("title") String title,
                     @Param("description") String description,
                     @Param("status") TicketStatus status,
                     @Param("priority") TicketPriority priority,
                     @Param("aiPrioritySuggestion") String aiPrioritySuggestion,
                     @Param("categoryId") Long categoryId,
                     @Param("departmentId") Long departmentId,
                     @Param("creatorId") Long creatorId,
                     @Param("assigneeId") Long assigneeId,
                     @Param("source") TicketSource source,
                     @Param("sourceSessionId") Long sourceSessionId,
                     @Param("sourceMessageId") Long sourceMessageId,
                     @Param("aiSummary") String aiSummary,
                     @Param("aiSuggestion") String aiSuggestion,
                     @Param("transferReason") String transferReason,
                     @Param("deadlineAt") LocalDateTime deadlineAt);

    int insertFlowLog(@Param("id") Long id,
                      @Param("ticketId") Long ticketId,
                      @Param("fromStatus") TicketStatus fromStatus,
                      @Param("toStatus") TicketStatus toStatus,
                      @Param("action") TicketWorkflowAction action,
                      @Param("operatorId") Long operatorId,
                      @Param("operatorRole") String operatorRole,
                      @Param("commentText") String commentText);

    Ticket findTicketForUpdate(@Param("ticketId") Long ticketId);

    Ticket findCreatedTicket(@Param("ticketId") Long ticketId, @Param("creatorId") Long creatorId);

    Ticket findAssignedTicket(@Param("ticketId") Long ticketId, @Param("assigneeId") Long assigneeId);

    Ticket findTicketById(@Param("ticketId") Long ticketId);

    List<Ticket> listCreatedTickets(@Param("creatorId") Long creatorId, @Param("limit") int limit);

    List<Ticket> listAssignedTickets(@Param("assigneeId") Long assigneeId, @Param("limit") int limit);

    List<Ticket> listManagedTickets(@Param("limit") int limit);

    List<TicketFlowLog> listFlowLogs(@Param("ticketId") Long ticketId);

    int insertComment(@Param("id") Long id,
                      @Param("ticketId") Long ticketId,
                      @Param("authorId") Long authorId,
                      @Param("commentType") TicketCommentType commentType,
                      @Param("content") String content,
                      @Param("internal") boolean internal);

    List<TicketComment> listComments(@Param("ticketId") Long ticketId,
                                     @Param("includeInternal") boolean includeInternal);

    int updateTicketStatus(@Param("ticketId") Long ticketId,
                           @Param("status") TicketStatus status,
                           @Param("assigneeId") Long assigneeId,
                           @Param("firstResolvedAt") LocalDateTime firstResolvedAt,
                           @Param("closedAt") LocalDateTime closedAt,
                           @Param("incrementReopen") Integer incrementReopen);
}
