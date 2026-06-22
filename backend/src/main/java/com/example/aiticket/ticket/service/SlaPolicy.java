package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.TicketPriority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class SlaPolicy {
    public LocalDateTime deadlineFor(TicketPriority priority, LocalDateTime createdAt) {
        return createdAt.plus(durationFor(priority));
    }

    public Duration durationFor(TicketPriority priority) {
        TicketPriority normalized = priority == null ? TicketPriority.NORMAL : priority;
        return switch (normalized) {
            case URGENT -> Duration.ofHours(4);
            case HIGH -> Duration.ofHours(8);
            case NORMAL -> Duration.ofHours(24);
            case LOW -> Duration.ofHours(72);
        };
    }
}
