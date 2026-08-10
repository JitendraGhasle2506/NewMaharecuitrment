package com.maharecruitment.gov.in.web.service.dashboard.model;

public record HRDesignationAttendanceView(
        Long designationId,
        String designationName,
        int totalEmployees,
        int presentEmployees,
        int absentEmployees,
        int presentPercent
) {
}
