package com.example.aiticket.ticket.web;

import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.SlaStatus;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;
import com.example.aiticket.ticket.service.SlaCalculator;
import com.example.aiticket.ticket.service.SlaSnapshot;

import java.time.LocalDateTime;

public record TicketResponse(
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
        LocalDateTime deadlineAt,
        SlaStatus slaStatus,
        Long slaRemainingMinutes,
        Integer reopenCount,
        LocalDateTime firstResolvedAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        SlaSnapshot sla = new SlaCalculator().snapshot(ticket.status(), ticket.deadlineAt());
        return new TicketResponse(
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
                ticket.deadlineAt(),
                sla.slaStatus(),
                sla.slaRemainingMinutes(),
                ticket.reopenCount(),
                ticket.firstResolvedAt(),
                ticket.closedAt(),
                ticket.createdAt(),
                ticket.updatedAt()
        );
    }
}
