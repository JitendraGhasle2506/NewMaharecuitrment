package com.maharecruitment.gov.in.web.service.dashboard;

import com.maharecruitment.gov.in.web.service.dashboard.model.RoleDashboardView;

public interface RoleLandingService {
    RoleDashboardView getDashboardByPath(String path);
}
