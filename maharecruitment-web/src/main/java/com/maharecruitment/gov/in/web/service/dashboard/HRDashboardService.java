package com.maharecruitment.gov.in.web.service.dashboard;

import java.util.Optional;

import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;

public interface HRDashboardService {
    HRDashboardView getDashboard();

    Optional<HRWingReportView> getWingReport(Long wingId);
}
