package com.example.aiticket.ticket.web;

import com.example.aiticket.ticket.domain.SlaStatus;
import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TicketResponseTest {
    @Test
    void exposesDerivedSlaFields() {
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(90);
        Ticket ticket = new Ticket(8L, "TK8", "标题", "描述", TicketStatus.PROCESSING,
                TicketPriority.HIGH, null, 1L, null, 4L, 3L, TicketSource.AI_SESSION,
                10L, 21L, "摘要", "建议", "原因", deadline, null, null, 0, false,
                LocalDateTime.now(), LocalDateTime.now());

        TicketResponse response = TicketResponse.from(ticket);

        assertThat(response.deadlineAt()).isEqualTo(deadline);
        assertThat(response.slaStatus()).isEqualTo(SlaStatus.DUE_SOON);
        assertThat(response.slaRemainingMinutes()).isNotNull();
    }
}
