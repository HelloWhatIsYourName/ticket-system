package com.example.aiticket.ticket.web;

import com.example.aiticket.ticket.domain.TicketComment;
import com.example.aiticket.ticket.domain.TicketCommentType;

import java.time.LocalDateTime;

public record TicketCommentResponse(
        Long id,
        Long ticketId,
        Long authorId,
        TicketCommentType commentType,
        String content,
        boolean internal,
        LocalDateTime createdAt
) {
    public static TicketCommentResponse from(TicketComment comment) {
        return new TicketCommentResponse(
                comment.id(),
                comment.ticketId(),
                comment.authorId(),
                comment.commentType(),
                comment.content(),
                comment.internal(),
                comment.createdAt()
        );
    }
}
