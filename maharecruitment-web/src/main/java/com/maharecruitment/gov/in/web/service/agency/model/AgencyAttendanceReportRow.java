package com.maharecruitment.gov.in.web.service.agency.model;

import java.util.Map;

public record AgencyAttendanceReportRow(
        String employeeType,
        Long employeeId,
        String employeeCode,
        String requestId,
        String employeeName,
        String designation,
        String department,
        String subDepartment,
        String projectName,
        String level,
        String agencyName,
        Map<Integer, String> dailyStatus,
        long presentCount,
        long absentCount,
        long leaveCount,
        long compOffCount,
        long tourCount,
        long holidayCount,
        long weekOffCount,
        long payableDays) {

    public long absentTotalCount() {
        return absentCount + leaveCount;
    }
}
