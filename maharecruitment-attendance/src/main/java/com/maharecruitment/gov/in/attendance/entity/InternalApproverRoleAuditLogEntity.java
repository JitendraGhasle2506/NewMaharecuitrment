package com.maharecruitment.gov.in.attendance.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.maharecruitment.gov.in.common.audit.AuditIpEntityListener;
import com.maharecruitment.gov.in.common.audit.IpAudited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row per change to the internal approver roles. Role lists are comma-separated role names. */
@Entity
@Table(name = "internal_approver_role_audit_log")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditIpEntityListener.class)
public class InternalApproverRoleAuditLogEntity implements IpAudited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    /** SEEDED (initial migration) or UPDATED (saved on the Admin screen). */
    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    @Column(name = "roles_added", length = 500)
    private String rolesAdded;

    @Column(name = "roles_removed", length = 500)
    private String rolesRemoved;

    @Column(name = "roles_before", length = 1000)
    private String rolesBefore;

    @Column(name = "roles_after", nullable = false, length = 1000)
    private String rolesAfter;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "actor_login_id", length = 150)
    private String actorLoginId;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
