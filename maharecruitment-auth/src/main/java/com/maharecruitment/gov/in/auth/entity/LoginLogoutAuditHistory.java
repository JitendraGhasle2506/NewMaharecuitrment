package com.maharecruitment.gov.in.auth.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "login_logout_audit_history",
        indexes = {
                @Index(name = "idx_login_logout_audit_user_time", columnList = "user_id,event_time"),
                @Index(name = "idx_login_logout_audit_username_time", columnList = "username,event_time"),
                @Index(name = "idx_login_logout_audit_session", columnList = "session_id_hash")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_login_logout_audit_event_session",
                columnNames = {"event_type", "session_id_hash"}))
public class LoginLogoutAuditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private LoginLogoutAuditEventType eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "session_id_hash", length = 64)
    private String sessionIdHash;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "authentication_method", length = 20)
    private String authenticationMethod;

    @Column(name = "logout_reason", length = 40)
    private String logoutReason;

    @Column(name = "failure_reason", length = 64)
    private String failureReason;

    @Column(name = "source", nullable = false, length = 20)
    private String source;
}
