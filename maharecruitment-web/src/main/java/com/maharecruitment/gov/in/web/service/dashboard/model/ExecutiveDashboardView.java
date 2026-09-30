package com.maharecruitment.gov.in.web.service.dashboard.model;

public record ExecutiveDashboardView(
        long totalEmployees,
        long internalEmployees,
        long mahaitEmployees,
        long externalEmployees,
        long totalProjects,
        long totalCells) {
}
