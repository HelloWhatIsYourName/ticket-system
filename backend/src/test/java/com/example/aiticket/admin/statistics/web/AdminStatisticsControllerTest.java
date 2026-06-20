package com.example.aiticket.admin.statistics.web;

import com.example.aiticket.admin.statistics.domain.AdminDashboardOverview;
import com.example.aiticket.admin.statistics.domain.HotQuestionStat;
import com.example.aiticket.admin.statistics.domain.TicketCategoryStat;
import com.example.aiticket.admin.statistics.service.AdminStatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminStatisticsControllerTest {
    @Test
    void endpointsKeepExpectedPermissions() throws Exception {
        assertThat(method("overview").getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('dashboard:view')");
        assertThat(method("ticketCategoryStats", int.class).getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('dashboard:view')");
        assertThat(method("hotQuestions", int.class).getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('dashboard:view')");
    }

    @Test
    void endpointsMapServiceResponses() {
        FakeAdminStatisticsService service = new FakeAdminStatisticsService();
        AdminStatisticsController controller = new AdminStatisticsController(service);

        AdminDashboardOverviewResponse overview = controller.overview().data();
        List<TicketCategoryStatResponse> categories = controller.ticketCategoryStats(5).data();
        List<HotQuestionStatResponse> hotQuestions = controller.hotQuestions(3).data();

        assertThat(overview.totalTickets()).isEqualTo(12);
        assertThat(overview.knowledgeHitRate()).isEqualTo(0.75);
        assertThat(categories).hasSize(1);
        assertThat(categories.getFirst().categoryName()).isEqualTo("通用问题");
        assertThat(service.lastCategoryLimit).isEqualTo(5);
        assertThat(hotQuestions).hasSize(1);
        assertThat(hotQuestions.getFirst().askCount()).isEqualTo(3);
        assertThat(service.lastHotQuestionLimit).isEqualTo(3);
        assertThat(Arrays.stream(AdminDashboardOverviewResponse.class.getRecordComponents())
                .map(RecordComponent::getName))
                .contains("totalTickets", "knowledgeHitRate")
                .doesNotContain("total_tickets", "knowledge_hit_rate");
    }

    private Method method(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return AdminStatisticsController.class.getMethod(name, parameterTypes);
    }

    private static final class FakeAdminStatisticsService extends AdminStatisticsService {
        private int lastCategoryLimit;
        private int lastHotQuestionLimit;

        private FakeAdminStatisticsService() {
            super(null);
        }

        @Override
        public AdminDashboardOverview overview() {
            return new AdminDashboardOverview(12, 3, 4, 2, 3, 5.5, 8, 20, 0.75);
        }

        @Override
        public List<TicketCategoryStat> ticketCategoryStats(int limit) {
            this.lastCategoryLimit = limit;
            return List.of(new TicketCategoryStat(1L, "通用问题", 5));
        }

        @Override
        public List<HotQuestionStat> hotQuestions(int limit) {
            this.lastHotQuestionLimit = limit;
            return List.of(new HotQuestionStat("忘记密码怎么办？", 3));
        }
    }
}
