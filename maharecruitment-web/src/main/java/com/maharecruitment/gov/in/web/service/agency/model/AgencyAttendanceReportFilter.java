package com.maharecruitment.gov.in.web.service.agency.model;

public record AgencyAttendanceReportFilter(
        int month,
        int year,
        String employeeType,
        String searchText) {
}
