package com.maharecruitment.gov.in.web.service.dashboard.model;

public record HRAttendanceSummaryView(
        int checkedInEmployees,
        int earlyCheckIns,
        int standardCheckIns,
        int lateCheckIns
) {
}
