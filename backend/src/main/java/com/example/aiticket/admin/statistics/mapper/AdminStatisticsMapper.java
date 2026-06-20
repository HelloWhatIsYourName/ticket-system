package com.example.aiticket.admin.statistics.mapper;

import com.example.aiticket.admin.statistics.domain.AdminDashboardOverview;
import com.example.aiticket.admin.statistics.domain.HotQuestionStat;
import com.example.aiticket.admin.statistics.domain.TicketCategoryStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminStatisticsMapper {
    AdminDashboardOverview selectOverview();

    List<TicketCategoryStat> selectTicketCategoryStats(@Param("limit") int limit);

    List<HotQuestionStat> selectHotQuestions(@Param("limit") int limit);
}
