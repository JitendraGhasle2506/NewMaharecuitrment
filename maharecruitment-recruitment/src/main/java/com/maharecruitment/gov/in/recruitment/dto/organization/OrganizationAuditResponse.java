package com.maharecruitment.gov.in.recruitment.dto.organization;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationAuditAction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationAuditResponse {

    private Long auditId;
    private OrganizationAuditAction actionType;
    private String entityType;
    private String entityId;
    private String actorLoginId;
    private String summary;
    private String details;
    private LocalDateTime occurredAt;
}
