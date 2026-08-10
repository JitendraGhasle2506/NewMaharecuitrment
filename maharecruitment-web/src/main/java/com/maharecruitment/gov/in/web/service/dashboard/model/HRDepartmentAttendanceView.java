package com.maharecruitment.gov.in.web.service.dashboard.model;

public record HRDepartmentAttendanceView(
        Long departmentId,
        String departmentName,
        int totalEmployees,
        int presentEmployees,
        int absentEmployees,
        int presentPercent
) {
}
