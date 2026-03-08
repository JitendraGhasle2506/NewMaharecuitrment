package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.List;

public record HRDashboardView(
        int totalProjects,
        int onboardingThisMonth,
        int internalEmployees,
        int externalEmployees,
        int totalEmployees,
        int pendingApprovals,
        int openPositions,
        String attritionRate,
        int internalPercent,
        int externalPercent,
        List<DepartmentOnboardingView> departmentOnboarding,
        List<ProjectWorkforceView> projects
) {
}
