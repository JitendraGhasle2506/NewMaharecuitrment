package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.List;

public record MdWorkforceWingView(
        Long wingId,
        String wingName,
        int cellCount,
        int employeeCount,
        List<MdWorkforceCellView> cells) {
}
