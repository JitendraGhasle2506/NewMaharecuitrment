package com.maharecruitment.gov.in.auth.entity;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "user_department_mapping",
        indexes = {
                @Index(name = "idx_user_department_mapping_user", columnList = "user_id"),
                @Index(name = "idx_user_department_mapping_department", columnList = "department_registration_id"),
                @Index(name = "idx_user_department_mapping_active", columnList = "active")
        })
public class UserDepartmentMappingEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_department_mapping_id")
    private Long userDepartmentMappingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "department_registration_id", nullable = false)
    private DepartmentRegistrationEntity departmentRegistration;

    @Column(name = "primary_mapping", nullable = false)
    private Boolean primaryMapping = Boolean.TRUE;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;
}
