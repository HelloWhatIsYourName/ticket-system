package com.example.aiticket.ticket.domain;

import java.time.LocalDateTime;

public record TicketCategory(
        Long id,
        String name,
        Long parentId,
        Integer sortOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
