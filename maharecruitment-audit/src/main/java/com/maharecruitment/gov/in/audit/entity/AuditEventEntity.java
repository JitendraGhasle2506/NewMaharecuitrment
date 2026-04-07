package com.maharecruitment.gov.in.audit.entity;

import java.time.LocalDateTime;

import org.springframework.util.StringUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "audit_event",
        indexes = {
                @Index(name = "idx_audit_event_entity", columnList = "module_name, entity_type, entity_id"),
                @Index(name = "idx_audit_event_time", columnList = "occurred_at"),
                @Index(name = "idx_audit_event_action", columnList = "action_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_event_id")
    private Long auditEventId;

    @Column(name = "module_name", nullable = false, length = 80)
    private String moduleName;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 80)
    private String entityId;

    @Column(name = "action_type", nullable = false, length = 80)
    private String actionType;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_login_id", length = 255)
    private String actorLoginId;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "activity_summary", nullable = false, length = 255)
    private String activitySummary;

    @Column(name = "activity_details", columnDefinition = "TEXT")
    private String activityDetails;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void onCreate() {
        moduleName = normalize(moduleName);
        entityType = normalize(entityType);
        entityId = normalize(entityId);
        actionType = normalize(actionType);
        actorLoginId = normalize(actorLoginId);
        actorName = normalize(actorName);
        activitySummary = normalize(activitySummary);
        activityDetails = normalize(activityDetails);
        metadataJson = normalize(metadataJson);

        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
