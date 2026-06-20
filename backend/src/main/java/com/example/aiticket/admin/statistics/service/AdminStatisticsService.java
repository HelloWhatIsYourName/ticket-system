package com.example.aiticket.admin.statistics.service;

import com.example.aiticket.admin.statistics.domain.AdminDashboardOverview;
import com.example.aiticket.admin.statistics.domain.HotQuestionStat;
import com.example.aiticket.admin.statistics.domain.TicketCategoryStat;
import com.example.aiticket.admin.statistics.mapper.AdminStatisticsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminStatisticsService {
    private final AdminStatisticsMapper mapper;

    public AdminStatisticsService(AdminStatisticsMapper mapper) {
        this.mapper = mapper;
    }

    public AdminDashboardOverview overview() {
        return mapper.selectOverview();
    }

    public List<TicketCategoryStat> ticketCategoryStats(int limit) {
        return mapper.selectTicketCategoryStats(normalizedLimit(limit));
    }

    public List<HotQuestionStat> hotQuestions(int limit) {
        return mapper.selectHotQuestions(normalizedLimit(limit));
    }

    private int normalizedLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, 50);
    }
}
