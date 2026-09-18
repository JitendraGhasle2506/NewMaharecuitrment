package com.maharecruitment.gov.in.web.service.hr.model;

public record EmployeeProjectBulkMappingResult(
        int requestedCount,
        int changedCount,
        int unchangedCount) {
}
