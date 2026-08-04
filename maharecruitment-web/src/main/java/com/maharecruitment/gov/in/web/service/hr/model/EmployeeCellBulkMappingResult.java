package com.maharecruitment.gov.in.web.service.hr.model;

public record EmployeeCellBulkMappingResult(
        int requestedCount,
        int changedCount,
        int unchangedCount) {
}
