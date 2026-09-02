package com.maharecruitment.gov.in.attendance.service.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record TeamAttendanceOverview(
        YearMonth period,
        LocalDate statusDate,
        List<TeamAttendanceMemberView> members,
        long totalPresentDays,
        long totalAbsentDays,
        long totalLeaveDays,
        long totalTourDays,
        int attendanceRate) {

    public int teamSize() {
        return members.size();
    }
}
