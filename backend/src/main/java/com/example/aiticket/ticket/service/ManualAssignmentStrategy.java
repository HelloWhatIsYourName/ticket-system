package com.example.aiticket.ticket.service;

import org.springframework.stereotype.Component;

@Component
public class ManualAssignmentStrategy implements AssignmentStrategy {
    @Override
    public Long selectAssignee(TicketAssignmentContext context) {
        if (context.requestedAssigneeId() == null) {
            throw new TicketWorkflowException("assignee is required");
        }
        return context.requestedAssigneeId();
    }
}
