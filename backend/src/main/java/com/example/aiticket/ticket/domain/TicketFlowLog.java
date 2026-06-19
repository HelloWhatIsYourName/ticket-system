package com.example.aiticket.ticket.domain;

import java.time.LocalDateTime;

public record TicketFlowLog(
        Long id,
        Long ticketId,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        TicketWorkflowAction action,
        Long operatorId,
        String operatorRole,
        String commentText,
        LocalDateTime createdAt
) {
}
