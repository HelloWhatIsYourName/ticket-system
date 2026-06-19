package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.TicketPriority;

public record CreateTicketFromAiSessionCommand(
        Long sessionId,
        Long assistantMessageId,
        String title,
        String description,
        Long categoryId,
        TicketPriority priority,
        String transferReason
) {
}
