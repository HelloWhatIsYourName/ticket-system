package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.TicketCommentType;

public record AddTicketCommentCommand(
        TicketCommentType commentType,
        String content
) {
}
