package com.example.aiticket.admin.statistics.domain;

public record AdminDashboardOverview(
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
}
