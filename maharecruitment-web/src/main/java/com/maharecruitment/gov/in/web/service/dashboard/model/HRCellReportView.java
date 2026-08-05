package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.List;

public record HRCellReportView(
        Long cellId,
        String cellName,
        int projectCount,
        int employeeCount,
        int projectPercent,
        int employeePercent,
        List<HREmployeeHierarchyView> employees
) {
}
