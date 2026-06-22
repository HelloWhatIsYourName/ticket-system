package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.SlaStatus;
import com.example.aiticket.ticket.domain.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SlaCalculatorTest {
    private final SlaCalculator calculator = new SlaCalculator();
    private final LocalDateTime now = LocalDateTime.of(2026, 6, 23, 10, 0);

    @Test
    void treatsResolvedAndClosedAsCompletedWithNoRemainingMinutes() {
        LocalDateTime deadline = now.plusHours(3);

        assertThat(calculator.snapshot(TicketStatus.RESOLVED, deadline, now))
                .isEqualTo(new SlaSnapshot(deadline, SlaStatus.COMPLETED, null));
        assertThat(calculator.snapshot(TicketStatus.CLOSED, deadline, now))
                .isEqualTo(new SlaSnapshot(deadline, SlaStatus.COMPLETED, null));
    }

    @Test
    void handlesMissingDeadlineAsOnTrackWithoutRemainingMinutes() {
        assertThat(calculator.snapshot(TicketStatus.PROCESSING, null, now))
                .isEqualTo(new SlaSnapshot(null, SlaStatus.ON_TRACK, null));
    }

    @Test
    void classifiesOverdueOnlyWhenNowIsAfterDeadline() {
        LocalDateTime deadline = now.minusMinutes(1);

        assertThat(calculator.snapshot(TicketStatus.PROCESSING, deadline, now).slaStatus())
                .isEqualTo(SlaStatus.OVERDUE);
        assertThat(calculator.snapshot(TicketStatus.PROCESSING, deadline, now).slaRemainingMinutes())
                .isEqualTo(-1L);
    }

    @Test
    void classifiesExactDeadlineAsDueSoon() {
        assertThat(calculator.snapshot(TicketStatus.PROCESSING, now, now))
                .isEqualTo(new SlaSnapshot(now, SlaStatus.DUE_SOON, 0L));
    }

    @Test
    void classifiesExactlyTwoHoursRemainingAsDueSoon() {
        LocalDateTime deadline = now.plusMinutes(120);

        assertThat(calculator.snapshot(TicketStatus.PROCESSING, deadline, now))
                .isEqualTo(new SlaSnapshot(deadline, SlaStatus.DUE_SOON, 120L));
    }

    @Test
    void classifiesMoreThanTwoHoursRemainingAsOnTrack() {
        LocalDateTime deadline = now.plusMinutes(121);

        assertThat(calculator.snapshot(TicketStatus.PROCESSING, deadline, now))
                .isEqualTo(new SlaSnapshot(deadline, SlaStatus.ON_TRACK, 121L));
    }
}
