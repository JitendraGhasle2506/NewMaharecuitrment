package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MobileAttendanceResponse(
        boolean success,
        String message,
        Long attendanceId,
        Long employeeId,
        String employeeCode,
        LocalDate attendanceDate,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        String attendanceSource) {
}
