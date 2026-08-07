package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.time.LocalTime;

public record HRAttendanceEmployeeView(
        Long employeeId,
        String employeeCode,
        String fullName,
        String recruitmentType,
        String attendanceStatus,
        LocalTime checkInTime) {
}
