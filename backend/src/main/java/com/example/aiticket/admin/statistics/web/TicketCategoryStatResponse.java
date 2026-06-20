package com.example.aiticket.admin.statistics.web;

import com.example.aiticket.admin.statistics.domain.TicketCategoryStat;

public record TicketCategoryStatResponse(
        Long categoryId,
        String categoryName,
        long ticketCount
) {
    public static TicketCategoryStatResponse from(TicketCategoryStat stat) {
        return new TicketCategoryStatResponse(stat.categoryId(), stat.categoryName(), stat.ticketCount());
    }
}
