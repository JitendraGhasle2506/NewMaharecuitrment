package com.maharecruitment.gov.in.workorder.entity;

import java.time.LocalDate;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "work_order_employee_mapping",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_work_order_employee", columnNames = { "work_order_id", "employee_id" })
        },
        indexes = {
                @Index(name = "idx_work_order_employee_work_order", columnList = "work_order_id"),
                @Index(name = "idx_work_order_employee_employee", columnList = "employee_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderEmployeeMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_employee_mapping_id")
    private Long workOrderEmployeeMappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrderEntity workOrder;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "employee_name", nullable = false, length = 150)
    private String employeeName;

    @Column(name = "designation_name", length = 150)
    private String designationName;

    @Column(name = "level_code", length = 50)
    private String levelCode;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "employment_status", length = 20)
    private String employmentStatus;

    @Column(name = "monthly_rate", precision = 14, scale = 2)
    private java.math.BigDecimal monthlyRate;

    @Column(name = "agency_commission_percent", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal agencyCommissionPercent = java.math.BigDecimal.valueOf(10);

    @Column(name = "agency_commission_amount", precision = 14, scale = 2)
    private java.math.BigDecimal agencyCommissionAmount;

    @Column(name = "gst_percent", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal gstPercent = java.math.BigDecimal.valueOf(18);

    @Column(name = "gst_amount", precision = 14, scale = 2)
    private java.math.BigDecimal gstAmount;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private java.math.BigDecimal totalAmount;

    @PrePersist
    @PreUpdate
    void normalize() {
        employeeCode = trim(employeeCode);
        employeeName = trim(employeeName);
        designationName = trim(designationName);
        levelCode = trim(levelCode);
        employmentStatus = trimUpper(employmentStatus);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String trimUpper(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
