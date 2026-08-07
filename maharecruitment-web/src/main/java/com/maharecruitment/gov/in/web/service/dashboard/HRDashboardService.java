package com.maharecruitment.gov.in.web.service.dashboard;

import java.util.Optional;

import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRTodayAttendanceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;

public interface HRDashboardService {
    HRDashboardView getDashboard();

    HRTodayAttendanceView getTodayAttendance();

    Optional<HRWingReportView> getWingReport(Long wingId);
}
