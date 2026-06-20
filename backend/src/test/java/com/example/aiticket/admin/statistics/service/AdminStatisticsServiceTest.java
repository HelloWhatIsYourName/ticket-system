package com.example.aiticket.admin.statistics.service;

import com.example.aiticket.admin.statistics.domain.AdminDashboardOverview;
import com.example.aiticket.admin.statistics.domain.HotQuestionStat;
import com.example.aiticket.admin.statistics.domain.TicketCategoryStat;
import com.example.aiticket.admin.statistics.mapper.AdminStatisticsMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminStatisticsServiceTest {
    @Test
    void overviewReturnsMapperData() {
        FakeAdminStatisticsMapper mapper = new FakeAdminStatisticsMapper();
        AdminStatisticsService service = new AdminStatisticsService(mapper);

        AdminDashboardOverview overview = service.overview();

        assertThat(overview.totalTickets()).isEqualTo(12);
        assertThat(overview.pendingTickets()).isEqualTo(3);
        assertThat(overview.knowledgeHitRate()).isEqualTo(0.75);
    }

    @Test
    void listLimitsDefaultToTenAndCapAtFifty() {
        FakeAdminStatisticsMapper mapper = new FakeAdminStatisticsMapper();
        AdminStatisticsService service = new AdminStatisticsService(mapper);

        service.ticketCategoryStats(0);
        assertThat(mapper.lastCategoryLimit).isEqualTo(10);

        service.ticketCategoryStats(99);
        assertThat(mapper.lastCategoryLimit).isEqualTo(50);

        service.hotQuestions(-1);
        assertThat(mapper.lastHotQuestionLimit).isEqualTo(10);

        service.hotQuestions(88);
        assertThat(mapper.lastHotQuestionLimit).isEqualTo(50);
    }

    private static final class FakeAdminStatisticsMapper implements AdminStatisticsMapper {
        private int lastCategoryLimit;
        private int lastHotQuestionLimit;

        @Override
        public AdminDashboardOverview selectOverview() {
            return new AdminDashboardOverview(12, 3, 4, 2, 3, 5.5, 8, 20, 0.75);
        }

        @Override
        public List<TicketCategoryStat> selectTicketCategoryStats(int limit) {
            this.lastCategoryLimit = limit;
            return List.of(new TicketCategoryStat(1L, "通用问题", 5));
        }

        @Override
        public List<HotQuestionStat> selectHotQuestions(int limit) {
            this.lastHotQuestionLimit = limit;
            return List.of(new HotQuestionStat("忘记密码怎么办？", 3));
        }
    }
}
