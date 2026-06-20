package com.example.aiticket.admin.statistics.mapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdminStatisticsMapperXmlTest {
    @Test
    void adminStatisticsMapperDeclaresDashboardQueries() throws Exception {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/AdminStatisticsMapper.xml"));

        assertThat(mapper).contains("selectOverview");
        assertThat(mapper).contains("selectTicketCategoryStats");
        assertThat(mapper).contains("selectHotQuestions");
        assertThat(mapper).contains("COUNT(*)");
        assertThat(mapper).contains("AVG(");
        assertThat(mapper).contains("FROM ticket");
        assertThat(mapper).contains("FROM ai_message");
    }
}
