package com.example.aiticket.ticket.web;

import com.example.aiticket.ticket.domain.TicketCommentType;
import com.example.aiticket.ticket.service.AddTicketCommentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketCommentRequest(
        @NotNull TicketCommentType commentType,
        @NotBlank String content
) {
    public AddTicketCommentCommand toCommand() {
        return new AddTicketCommentCommand(commentType, content);
    }
}
