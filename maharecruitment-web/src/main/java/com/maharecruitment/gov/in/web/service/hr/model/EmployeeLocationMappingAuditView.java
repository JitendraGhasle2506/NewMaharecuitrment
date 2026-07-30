package com.maharecruitment.gov.in.web.service.hr.model;

import java.time.LocalDateTime;

public record EmployeeLocationMappingAuditView(
        String actionType,
        String actorLoginId,
        String previousLocationIds,
        String newLocationIds,
        String addedLocationIds,
        String removedLocationIds,
        String summary,
        String details,
        LocalDateTime occurredAt) {
}
