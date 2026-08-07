package com.maharecruitment.gov.in.web.service.dashboard.model;

public record HRWingDirectoryItemView(
        Long wingId,
        String wingName,
        int cellCount,
        int projectCount,
        int employeeCount
) {
}
