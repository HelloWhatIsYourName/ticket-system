package com.example.aiticket.ticket.service;

public interface AssignmentStrategy {
    Long selectAssignee(TicketAssignmentContext context);
}
