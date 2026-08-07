package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.List;

public record HRWingReportsView(
        int totalWings,
        int totalCells,
        int totalProjects,
        int totalEmployees,
        List<HRWingDirectoryItemView> wings
) {
}
