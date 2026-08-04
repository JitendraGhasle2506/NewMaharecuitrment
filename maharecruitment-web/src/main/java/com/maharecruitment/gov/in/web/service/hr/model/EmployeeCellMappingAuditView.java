package com.maharecruitment.gov.in.web.service.hr.model;

import java.time.LocalDateTime;

public record EmployeeCellMappingAuditView(
        String actionType,
        String actorLoginId,
        Long previousCellId,
        Long newCellId,
        String summary,
        String details,
        LocalDateTime occurredAt) {
}
