package com.maharecruitment.gov.in.master.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "agency_master_audit_log",
        indexes = {
                @Index(name = "idx_agency_audit_agency_id", columnList = "agency_id"),
                @Index(name = "idx_agency_audit_action_ts", columnList = "action_timestamp")
        })
public class AgencyMasterAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "agency_id", nullable = false)
    private Long agencyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private AgencyMasterAuditAction actionType;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username", length = 255)
    private String actorUsername;

    @CreationTimestamp
    @Column(name = "action_timestamp", nullable = false, updatable = false)
    private LocalDateTime actionTimestamp;

    @Lob
    @Column(name = "details")
    private String details;
}
