package com.example.aiticket.ticket.domain;

import java.time.LocalDateTime;

public record TicketComment(
        Long id,
        Long ticketId,
        Long authorId,
        TicketCommentType commentType,
        String content,
        boolean internal,
        LocalDateTime createdAt
) {
}
