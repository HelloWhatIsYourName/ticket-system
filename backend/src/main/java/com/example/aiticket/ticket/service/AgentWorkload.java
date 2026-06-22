package com.example.aiticket.ticket.service;

public record AgentWorkload(
        Long userId,
        String username,
        String displayName,
        Long activeTicketCount
) {
}
