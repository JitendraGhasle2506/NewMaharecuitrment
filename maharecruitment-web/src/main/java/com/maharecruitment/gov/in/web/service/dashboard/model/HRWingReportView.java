package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.List;

public record HRWingReportView(
        Long wingId,
        String wingName,
        int cellCount,
        int projectCount,
        int employeeCount,
        List<HRCellReportView> cells
) {
}
