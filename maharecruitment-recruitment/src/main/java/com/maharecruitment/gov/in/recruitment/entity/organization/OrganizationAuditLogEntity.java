package com.maharecruitment.gov.in.recruitment.entity.organization;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "team_management_audit_log",
        indexes = {
                @Index(name = "idx_team_mgmt_audit_entity", columnList = "entity_type,entity_id"),
                @Index(name = "idx_team_mgmt_audit_action", columnList = "action_type"),
                @Index(name = "idx_team_mgmt_audit_occurred", columnList = "occurred_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class OrganizationAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private OrganizationAuditAction actionType;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 80)
    private String entityId;

    @Column(name = "actor_login_id", length = 255)
    private String actorLoginId;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
        if (entityType != null) {
            entityType = entityType.trim();
        }
        if (entityId != null) {
            entityId = entityId.trim();
        }
        if (actorLoginId != null) {
            actorLoginId = actorLoginId.trim();
        }
        if (summary != null) {
            summary = summary.trim();
        }
        if (details != null) {
            details = details.trim();
        }
    }
}
