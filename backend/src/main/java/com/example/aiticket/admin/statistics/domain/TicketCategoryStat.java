package com.example.aiticket.admin.statistics.domain;

public record TicketCategoryStat(
        Long categoryId,
        String categoryName,
        long ticketCount
) {
}
