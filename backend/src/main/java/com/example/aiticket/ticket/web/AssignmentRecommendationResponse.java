package com.example.aiticket.ticket.web;

import com.example.aiticket.ticket.service.AssignmentRecommendation;

public record AssignmentRecommendationResponse(
        Long recommendedAssigneeId,
        String recommendedUsername,
        String recommendedDisplayName,
        Long activeTicketCount,
        String reason
) {
    public static AssignmentRecommendationResponse from(AssignmentRecommendation recommendation) {
        return new AssignmentRecommendationResponse(
                recommendation.recommendedAssigneeId(),
                recommendation.recommendedUsername(),
                recommendation.recommendedDisplayName(),
                recommendation.activeTicketCount(),
                recommendation.reason()
        );
    }
}
