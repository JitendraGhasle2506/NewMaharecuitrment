package com.maharecruitment.gov.in.web.service.agency.model;

public record AgencyAttendanceReportSummary(
        int employeeCount,
        int internalEmployeeCount,
        int externalEmployeeCount,
        long presentDays,
        long absentDays,
        long leaveDays,
        long compOffDays,
        long tourDays,
        long holidayDays,
        long weekOffDays,
        long payableDays) {
}
