package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.SlaStatus;

import java.time.LocalDateTime;

public record SlaSnapshot(
        LocalDateTime deadlineAt,
        SlaStatus slaStatus,
        Long slaRemainingMinutes
) {
}
