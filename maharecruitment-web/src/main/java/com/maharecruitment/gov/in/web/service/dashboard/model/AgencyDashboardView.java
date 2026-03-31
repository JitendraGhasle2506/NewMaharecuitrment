package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.List;

public record AgencyDashboardView(
        String agencyName,
        long totalOpenings,
        long candidatesSubmitted,
        long interviewsScheduled,
        long onboardedEmployees,
        String status,
        List<AgencyTaskView> recentNotifications
) {
}
