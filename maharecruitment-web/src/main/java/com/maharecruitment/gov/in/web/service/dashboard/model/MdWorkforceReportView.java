package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.List;

public record MdWorkforceReportView(
        int totalWings,
        int totalCells,
        int totalEmployees,
        List<MdWorkforceWingView> wings) {
}
