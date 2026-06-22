package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.TicketPriority;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SlaPolicyTest {
    private final SlaPolicy policy = new SlaPolicy();
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 23, 9, 0);

    @Test
    void calculatesDeadlineByPriority() {
        assertThat(policy.deadlineFor(TicketPriority.URGENT, createdAt)).isEqualTo(createdAt.plusHours(4));
        assertThat(policy.deadlineFor(TicketPriority.HIGH, createdAt)).isEqualTo(createdAt.plusHours(8));
        assertThat(policy.deadlineFor(TicketPriority.NORMAL, createdAt)).isEqualTo(createdAt.plusHours(24));
        assertThat(policy.deadlineFor(TicketPriority.LOW, createdAt)).isEqualTo(createdAt.plusHours(72));
    }

    @Test
    void defaultsMissingPriorityToNormal() {
        assertThat(policy.deadlineFor(null, createdAt)).isEqualTo(createdAt.plusHours(24));
    }
}
