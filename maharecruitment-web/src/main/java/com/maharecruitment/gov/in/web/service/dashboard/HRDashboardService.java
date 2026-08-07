package com.maharecruitment.gov.in.web.service.dashboard;

import java.util.Optional;

import com.maharecruitment.gov.in.web.service.dashboard.model.HRAttendanceDetailView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRTodayAttendanceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportsView;

public interface HRDashboardService {
    HRDashboardView getDashboard();

    HRTodayAttendanceView getTodayAttendance();

    HRAttendanceDetailView getTodayAttendanceDetails(String category, Long cellId, int page, int size);

    HRWingReportsView getWingReports();

    Optional<HRWingReportView> getWingReport(Long wingId);
}
