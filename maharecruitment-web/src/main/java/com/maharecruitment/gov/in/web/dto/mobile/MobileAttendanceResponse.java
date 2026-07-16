package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.LocalDate;
import java.time.LocalTime;

public record MobileAttendanceResponse(
        boolean success,
        String message,
        Long attendanceId,
        Long employeeId,
        String employeeCode,
        LocalDate attendanceDate,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        String attendanceSource) {
}
