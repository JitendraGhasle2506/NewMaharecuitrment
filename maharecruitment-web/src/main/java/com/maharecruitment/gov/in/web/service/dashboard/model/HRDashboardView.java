package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.List;

public record HRDashboardView(
        int totalProjects,
        int internalProjects,
        int externalProjects,
        int onboardingThisMonth,
        int internalEmployees,
        int externalEmployees,
        int mahaitEmployees,
        int totalEmployees,
        int presentEmployees,
        int absentEmployees,
        int internalPresentEmployees,
        int mahaitPresentEmployees,
        int externalPresentEmployees,
        int presentPercent,
        HRAttendanceSummaryView attendanceSummary,
        int pendingApprovals,
        int totalWings,
        int totalCells,
        int internalPercent,
        int externalPercent,
        List<DepartmentOnboardingView> departmentOnboarding,
        List<ProjectScopeListItemView> internalProjectList,
        List<ProjectScopeListItemView> externalProjectList
) {
}
