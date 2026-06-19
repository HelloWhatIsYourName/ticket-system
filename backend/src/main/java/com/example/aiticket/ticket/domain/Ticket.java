package com.example.aiticket.ticket.domain;

import java.time.LocalDateTime;

public record Ticket(
        Long id,
        String ticketNo,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String aiPrioritySuggestion,
        Long categoryId,
        Long departmentId,
        Long creatorId,
        Long assigneeId,
        TicketSource source,
        Long sourceSessionId,
        Long sourceMessageId,
        String aiSummary,
        String aiSuggestion,
        String transferReason,
        LocalDateTime deadlineAt,
        LocalDateTime firstResolvedAt,
        LocalDateTime closedAt,
        Integer reopenCount,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
