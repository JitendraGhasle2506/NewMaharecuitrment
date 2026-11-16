package com.maharecruitment.gov.in.attendance.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A role whose holders may approve or reject leave/tour requests of INTERNAL employees.
 * Maintained on Admin > Internal Approver Roles; every change is written to internal_approver_role_audit_log.
 */
@Entity
@Table(name = "internal_approver_role")
@Getter
@Setter
@NoArgsConstructor
public class InternalApproverRoleEntity {

    @Id
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "granted_by_user_id")
    private Long grantedByUserId;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    public InternalApproverRoleEntity(Long roleId, Long grantedByUserId) {
        this.roleId = roleId;
        this.grantedByUserId = grantedByUserId;
    }
}
