package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.SlaStatus;
import com.example.aiticket.ticket.domain.TicketStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class SlaCalculator {
    private static final long DUE_SOON_MINUTES = 120L;

    public SlaSnapshot snapshot(TicketStatus status, LocalDateTime deadlineAt) {
        return snapshot(status, deadlineAt, LocalDateTime.now());
    }

    SlaSnapshot snapshot(TicketStatus status, LocalDateTime deadlineAt, LocalDateTime now) {
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            return new SlaSnapshot(deadlineAt, SlaStatus.COMPLETED, null);
        }
        if (deadlineAt == null) {
            return new SlaSnapshot(null, SlaStatus.ON_TRACK, null);
        }

        long remainingMinutes = Duration.between(now, deadlineAt).toMinutes();
        if (now.isAfter(deadlineAt)) {
            return new SlaSnapshot(deadlineAt, SlaStatus.OVERDUE, remainingMinutes);
        }
        if (remainingMinutes <= DUE_SOON_MINUTES) {
            return new SlaSnapshot(deadlineAt, SlaStatus.DUE_SOON, remainingMinutes);
        }
        return new SlaSnapshot(deadlineAt, SlaStatus.ON_TRACK, remainingMinutes);
    }
}
