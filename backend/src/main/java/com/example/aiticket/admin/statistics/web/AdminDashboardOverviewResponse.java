package com.example.aiticket.admin.statistics.web;

import com.example.aiticket.admin.statistics.domain.AdminDashboardOverview;

public record AdminDashboardOverviewResponse(
        long totalTickets,
        long pendingTickets,
        long processingTickets,
        long resolvedTickets,
        long closedTickets,
        double averageResolveHours,
        long knowledgeDocuments,
        long aiQuestions,
        double knowledgeHitRate
) {
    public static AdminDashboardOverviewResponse from(AdminDashboardOverview overview) {
        return new AdminDashboardOverviewResponse(
                overview.totalTickets(),
                overview.pendingTickets(),
                overview.processingTickets(),
                overview.resolvedTickets(),
                overview.closedTickets(),
                overview.averageResolveHours(),
                overview.knowledgeDocuments(),
                overview.aiQuestions(),
                overview.knowledgeHitRate()
        );
    }
}
