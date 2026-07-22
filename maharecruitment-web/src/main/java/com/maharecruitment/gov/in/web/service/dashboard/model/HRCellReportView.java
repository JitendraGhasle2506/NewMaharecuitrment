package com.maharecruitment.gov.in.web.service.dashboard.model;

public record HRCellReportView(
        Long cellId,
        String cellName,
        int projectCount,
        int employeeCount,
        int projectPercent,
        int employeePercent
) {
}
