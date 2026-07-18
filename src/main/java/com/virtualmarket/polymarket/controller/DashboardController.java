package com.virtualmarket.polymarket.controller;

import com.virtualmarket.polymarket.dto.DashboardSummaryResponse;
import com.virtualmarket.polymarket.dto.AdminDashboardResponse;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.service.StatisticsService;
import com.virtualmarket.polymarket.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Platform and user summary statistics")
public class DashboardController {

    private final StatisticsService statisticsService;
    private final AdminDashboardService adminDashboardService;

    public DashboardController(StatisticsService statisticsService, AdminDashboardService adminDashboardService) {
        this.statisticsService = statisticsService;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary statistics")
    public DashboardSummaryResponse getDashboardSummary(Authentication authentication) {
        User user = authentication != null && authentication.getPrincipal() instanceof User
                ? (User) authentication.getPrincipal()
                : null;
        return statisticsService.getDashboardSummary(user);
    }

    @GetMapping("/admin")
    @Operation(summary = "Get the admin operations dashboard")
    public AdminDashboardResponse getAdminDashboard() {
        return adminDashboardService.getDashboard();
    }
}
