package com.example.aiticket.ticket.web;

import com.example.aiticket.ticket.domain.TicketFlowLog;
import com.example.aiticket.ticket.domain.TicketStatus;
import com.example.aiticket.ticket.domain.TicketWorkflowAction;

import java.time.LocalDateTime;

public record TicketFlowLogResponse(
        Long id,
        Long ticketId,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        TicketWorkflowAction action,
        Long operatorId,
        String operatorRole,
        String commentText,
        LocalDateTime createdAt
) {
    public static TicketFlowLogResponse from(TicketFlowLog flowLog) {
        return new TicketFlowLogResponse(
                flowLog.id(),
                flowLog.ticketId(),
                flowLog.fromStatus(),
                flowLog.toStatus(),
                flowLog.action(),
                flowLog.operatorId(),
                flowLog.operatorRole(),
                flowLog.commentText(),
                flowLog.createdAt()
        );
    }
}
