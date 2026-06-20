package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.Ticket;

public record TicketAssignmentContext(
        Ticket ticket,
        Long operatorId,
        String operatorRole,
        Long requestedAssigneeId,
        String comment
) {
}
