package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualAssignmentStrategyTest {
    private final ManualAssignmentStrategy strategy = new ManualAssignmentStrategy();

    @Test
    void selectsRequestedAssigneeForManualAssignment() {
        TicketAssignmentContext context = new TicketAssignmentContext(ticket(), 2L, "ADMIN", 3L, "分配给演示坐席");

        assertThat(strategy.selectAssignee(context)).isEqualTo(3L);
    }

    @Test
    void rejectsMissingManualAssignee() {
        TicketAssignmentContext context = new TicketAssignmentContext(ticket(), 2L, "ADMIN", null, "缺少坐席");

        assertThatThrownBy(() -> strategy.selectAssignee(context))
                .isInstanceOf(TicketWorkflowException.class)
                .hasMessage("assignee is required");
    }

    private Ticket ticket() {
        return new Ticket(100L, "TK100", "标题", "描述", TicketStatus.PENDING_ASSIGN,
                TicketPriority.NORMAL, null, 1L, null, 7L, null, TicketSource.AI_SESSION,
                10L, 21L, "摘要", "建议", "原因", null, null, null, 0, false,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
