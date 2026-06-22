package com.example.aiticket.ticket.service;

public record AssignmentRecommendation(
        Long recommendedAssigneeId,
        String recommendedUsername,
        String recommendedDisplayName,
        Long activeTicketCount,
        String reason
) {
    public static AssignmentRecommendation empty(String reason) {
        return new AssignmentRecommendation(null, null, null, null, reason);
    }
}
