package com.maharecruitment.gov.in.web.service.dashboard;

import java.util.Optional;

import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRTodayAttendanceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportsView;

public interface HRDashboardService {
    HRDashboardView getDashboard();

    HRTodayAttendanceView getTodayAttendance();

    HRWingReportsView getWingReports();

    Optional<HRWingReportView> getWingReport(Long wingId);
}
