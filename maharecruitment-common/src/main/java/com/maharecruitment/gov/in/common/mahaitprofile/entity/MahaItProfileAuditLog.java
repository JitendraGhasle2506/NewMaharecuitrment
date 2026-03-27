package com.maharecruitment.gov.in.common.mahaitprofile.entity;

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
@Table(name = "mahait_profile_master_audit_log", indexes = {
                @Index(name = "idx_mahait_profile_audit_profile_id", columnList = "mahait_profile_id"),
                @Index(name = "idx_mahait_profile_audit_action_ts", columnList = "action_timestamp")
})
public class MahaItProfileAuditLog {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "audit_id")
        private Long auditId;

        @Column(name = "mahait_profile_id", nullable = false)
        private Long mahaItProfileId;

        @Enumerated(EnumType.STRING)
        @Column(name = "action_type", nullable = false, length = 30)
        private MahaItProfileAuditAction actionType;

        @Column(name = "actor_user_id")
        private Long actorUserId;

        @Column(name = "actor_username", nullable = false, length = 255)
        private String actorUsername;

        @CreationTimestamp
        @Column(name = "action_timestamp", nullable = false, updatable = false)
        private LocalDateTime actionTimestamp;

        @Lob
        @Column(name = "details")
        private String details;
}
