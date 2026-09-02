package com.maharecruitment.gov.in.attendance.service.model;

public record TeamAttendanceMemberView(
        Long employeeId,
        String employeeCode,
        String employeeName,
        String initials,
        String designation,
        String unitName,
        String projectName,
        String latestStatus,
        String latestInTime,
        String latestOutTime,
        long presentDays,
        long absentDays,
        long leaveDays,
        long tourDays,
        long holidayDays,
        long weekOffDays,
        int attendanceRate) {
}
