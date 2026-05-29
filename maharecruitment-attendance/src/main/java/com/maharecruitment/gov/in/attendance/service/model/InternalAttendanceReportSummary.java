package com.maharecruitment.gov.in.attendance.service.model;

import lombok.Data;

@Data
public class InternalAttendanceReportSummary {

    private long employeeCount;
    private long totalDaysInMonth;
    private long officeDayCount;
    private long totalHolidayCount;
    private long totalWeekOffCount;
    private long presentCount;
    private long absentCount;
    private long leaveCount;
    private long compOffCount;
    private long holidayCount;
    private long weekOffCount;
    private long tourCount;
    private long payableDays;

    public long getAbsentTotalCount() {
        return absentCount + leaveCount;
    }
}
