package com.example.aiticket.admin.statistics.web;

import com.example.aiticket.admin.statistics.service.AdminStatisticsService;
import com.example.aiticket.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {
    private final AdminStatisticsService service;

    public AdminStatisticsController(AdminStatisticsService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<AdminDashboardOverviewResponse> overview() {
        return ApiResponse.ok(AdminDashboardOverviewResponse.from(service.overview()));
    }

    @GetMapping("/ticket-categories")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<List<TicketCategoryStatResponse>> ticketCategoryStats(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(service.ticketCategoryStats(limit).stream()
                .map(TicketCategoryStatResponse::from)
                .toList());
    }

    @GetMapping("/hot-questions")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<List<HotQuestionStatResponse>> hotQuestions(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(service.hotQuestions(limit).stream()
                .map(HotQuestionStatResponse::from)
                .toList());
    }
}
