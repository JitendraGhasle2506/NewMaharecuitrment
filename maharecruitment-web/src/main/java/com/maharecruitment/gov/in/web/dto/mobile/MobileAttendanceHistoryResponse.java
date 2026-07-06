package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MobileAttendanceHistoryResponse(
        boolean success,
        String message,
        Long employeeId,
        LocalDate fromDate,
        LocalDate toDate,
        List<AttendanceEntry> attendanceHistory) {

    public MobileAttendanceHistoryResponse {
        attendanceHistory = attendanceHistory == null ? List.of() : List.copyOf(attendanceHistory);
    }

    public record AttendanceEntry(
            Long attendanceId,
            LocalDate attendanceDate,
            LocalDateTime checkInTime,
            LocalDateTime checkOutTime,
            String inTime,
            String outTime,
            String totalHours,
            String status,
            String attendanceSource,
            boolean checkedIn,
            boolean checkedOut) {
    }
}
