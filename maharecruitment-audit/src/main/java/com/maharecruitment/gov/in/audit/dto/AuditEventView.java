package com.maharecruitment.gov.in.audit.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventView {

    private Long auditEventId;
    private String moduleName;
    private String entityType;
    private String entityId;
    private String actionType;
    private Long actorUserId;
    private String actorLoginId;
    private String actorName;
    private String activitySummary;
    private String activityDetails;
    private String metadataJson;
    private LocalDateTime occurredAt;
}
