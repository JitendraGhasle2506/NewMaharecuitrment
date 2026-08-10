package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.time.LocalDate;
import java.util.List;

public record HRTodayAttendanceView(
        LocalDate attendanceDate,
        int totalEmployees,
        int presentEmployees,
        int absentEmployees,
        int presentPercent,
        HRAttendanceSummaryView checkIns,
        HRAttendanceGrouping grouping,
        List<HRCellAttendanceView> cells,
        List<HRDesignationAttendanceView> designations,
        List<HRDepartmentAttendanceView> departments
) {
}
