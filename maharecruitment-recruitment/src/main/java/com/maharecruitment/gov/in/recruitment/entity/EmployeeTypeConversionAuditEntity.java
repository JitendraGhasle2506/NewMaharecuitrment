package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "employee_type_conversion_audit", indexes = {
        @Index(name = "idx_emp_type_conversion_employee", columnList = "employee_id"),
        @Index(name = "idx_emp_type_conversion_occurred", columnList = "occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
public class EmployeeTypeConversionAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name", nullable = false, length = 150)
    private String employeeName;

    @Column(name = "employee_code", length = 50)
    private String employeeCode;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "department_name", length = 100)
    private String departmentName;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "location_name", length = 320)
    private String locationName;

    @Column(name = "cell_id")
    private Long cellId;

    @Column(name = "cell_name", length = 180)
    private String cellName;

    @Column(name = "reporting_hod_user_id")
    private Long reportingHodUserId;

    @Column(name = "reporting_hod_name", length = 255)
    private String reportingHodName;

    @Column(name = "manager_type", length = 10)
    private String managerType;

    @Column(name = "reporting_manager_employee_id")
    private Long reportingManagerEmployeeId;

    @Column(name = "reporting_manager_name", length = 255)
    private String reportingManagerName;

    @Column(name = "actor_login_id", nullable = false, length = 255)
    private String actorLoginId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
