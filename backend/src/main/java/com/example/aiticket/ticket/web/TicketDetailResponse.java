package com.example.aiticket.ticket.web;

import com.example.aiticket.ticket.domain.TicketDetail;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TicketDetailResponse(
        Long id,
        String ticketNo,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        Long categoryId,
        Long creatorId,
        Long assigneeId,
        TicketSource source,
        Long sourceSessionId,
        Long sourceMessageId,
        String aiSummary,
        String aiSuggestion,
        String transferReason,
        Integer reopenCount,
        LocalDateTime firstResolvedAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TicketFlowLogResponse> flowLogs
) {
    public static TicketDetailResponse from(TicketDetail detail) {
        TicketResponse ticket = TicketResponse.from(detail.ticket());
        return new TicketDetailResponse(
                ticket.id(),
                ticket.ticketNo(),
                ticket.title(),
                ticket.description(),
                ticket.status(),
                ticket.priority(),
                ticket.categoryId(),
                ticket.creatorId(),
                ticket.assigneeId(),
                ticket.source(),
                ticket.sourceSessionId(),
                ticket.sourceMessageId(),
                ticket.aiSummary(),
                ticket.aiSuggestion(),
                ticket.transferReason(),
                ticket.reopenCount(),
                ticket.firstResolvedAt(),
                ticket.closedAt(),
                ticket.createdAt(),
                ticket.updatedAt(),
                detail.flowLogs().stream().map(TicketFlowLogResponse::from).toList()
        );
    }
}
