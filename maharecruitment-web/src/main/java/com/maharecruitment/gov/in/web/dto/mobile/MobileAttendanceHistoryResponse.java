package com.maharecruitment.gov.in.web.dto.mobile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
            LocalTime checkInTime,
            LocalTime checkOutTime,
            BigDecimal checkInLatitude,
            BigDecimal checkInLongitude,
            String checkInLocationAddress,
            BigDecimal checkOutLatitude,
            BigDecimal checkOutLongitude,
            String checkOutLocationAddress,
            String inTime,
            String outTime,
            String totalHours,
            String status,
            String attendanceSource,
            boolean checkedIn,
            boolean checkedOut) {
    }
}
