package com.maharecruitment.gov.in.attendance.dto;

import java.time.LocalDateTime;

/**
 * A leave/tour decision on an INTERNAL employee's request, with the roles the approver held at that moment.
 * Read from attendance_request_audit_log.
 */
public record InternalDecisionLogView(
        LocalDateTime occurredAt,
        String requestType,
        Long requestId,
        String employeeCode,
        String employeeName,
        String actionType,
        String actorName,
        String actorLoginId,
        String actorRoles,
        String remarks,
        String channel,
        String ipAddress) {
}
