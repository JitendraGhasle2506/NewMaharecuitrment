package com.maharecruitment.gov.in.web.service.dashboard.model;

public record HRCellAttendanceView(
        Long cellId,
        String cellName,
        String wingName,
        int totalEmployees,
        int presentEmployees,
        int absentEmployees,
        int presentPercent
) {
}
