package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.List;

public record MdWorkforceCellView(
        Long cellId,
        String cellName,
        int employeeCount,
        List<MdWorkforceEmployeeView> employees) {
}
