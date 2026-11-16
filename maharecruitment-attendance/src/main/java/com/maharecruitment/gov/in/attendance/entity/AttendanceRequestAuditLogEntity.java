package com.maharecruitment.gov.in.attendance.entity;

import java.time.LocalDate;
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

/** One row per leave/tour request event: apply, forward, approve, reject or cancel. */
@Entity
@Table(name = "attendance_request_audit_log")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditIpEntityListener.class)
public class AttendanceRequestAuditLogEntity implements IpAudited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    @Column(name = "request_type", nullable = false, length = 10)
    private String requestType;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    @Column(name = "previous_stage", length = 20)
    private String previousStage;

    @Column(name = "new_stage", length = 20)
    private String newStage;

    @Column(name = "request_category", length = 100)
    private String requestCategory;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "actor_login_id", length = 150)
    private String actorLoginId;

    @Column(name = "actor_designation", length = 150)
    private String actorDesignation;

    @Column(name = "actor_roles", length = 500)
    private String actorRoles;

    @Column(name = "channel", nullable = false, length = 10)
    private String channel;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "details", length = 2000)
    private String details;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
