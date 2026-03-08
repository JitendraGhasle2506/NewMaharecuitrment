package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.List;

public record RoleDashboardView(
        String title,
        String subtitle,
        int activeProjects,
        int newOnboarding,
        int internalCount,
        int externalCount,
        int pendingApprovals,
        int alerts,
        List<RoleTaskView> tasks
) {
}
