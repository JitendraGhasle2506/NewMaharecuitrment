package com.maharecruitment.gov.in.web.service.dashboard.model;

public record HREmployeeHierarchyView(
        Long employeeId,
        String employeeCode,
        String employeeName,
        String initials,
        String photoPath,
        String designationName,
        int depth,
        int directReportCount,
        String reportsToName,
        String reportingSource
) {
}
