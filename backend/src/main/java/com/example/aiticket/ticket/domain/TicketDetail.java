package com.example.aiticket.ticket.domain;

import java.util.List;

public record TicketDetail(
        Ticket ticket,
        List<TicketFlowLog> flowLogs
) {
}
