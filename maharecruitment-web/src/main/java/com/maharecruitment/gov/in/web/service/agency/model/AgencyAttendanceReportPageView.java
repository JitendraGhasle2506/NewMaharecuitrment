package com.maharecruitment.gov.in.web.service.agency.model;

import java.util.List;
import java.util.Map;

public record AgencyAttendanceReportPageView(
        Long agencyId,
        String agencyName,
        AgencyAttendanceReportFilter filter,
        Map<Integer, String> monthNames,
        List<Integer> yearOptions,
        int daysInMonth,
        List<String> employeeTypeOptions,
        List<AgencyAttendanceReportRow> rows,
        AgencyAttendanceReportSummary summary) {
}
