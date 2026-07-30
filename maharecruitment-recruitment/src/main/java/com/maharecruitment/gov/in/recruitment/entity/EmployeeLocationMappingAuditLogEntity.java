package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "employee_location_mapping_audit_log",
        indexes = {
                @Index(name = "idx_emp_location_audit_employee", columnList = "employee_id"),
                @Index(name = "idx_emp_location_audit_actor", columnList = "actor_login_id"),
                @Index(name = "idx_emp_location_audit_occurred", columnList = "occurred_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class EmployeeLocationMappingAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(name = "actor_login_id", length = 255)
    private String actorLoginId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "previous_location_ids", columnDefinition = "TEXT")
    private String previousLocationIds;

    @Column(name = "new_location_ids", columnDefinition = "TEXT")
    private String newLocationIds;

    @Column(name = "added_location_ids", columnDefinition = "TEXT")
    private String addedLocationIds;

    @Column(name = "removed_location_ids", columnDefinition = "TEXT")
    private String removedLocationIds;

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
        actorLoginId = trim(actorLoginId);
        actionType = trim(actionType);
        previousLocationIds = trim(previousLocationIds);
        newLocationIds = trim(newLocationIds);
        addedLocationIds = trim(addedLocationIds);
        removedLocationIds = trim(removedLocationIds);
        summary = trim(summary);
        details = trim(details);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
