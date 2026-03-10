package com.maharecruitment.gov.in.department.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "department_project_resource_requirement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentProjectResourceRequirementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_project_resource_requirement_id")
    private Long departmentProjectResourceRequirementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_project_application_id", nullable = false)
    private DepartmentProjectApplicationEntity application;

    @Column(name = "designation_id", nullable = false)
    private Long designationId;

    @Column(name = "designation_name", nullable = false, length = 200)
    private String designationName;

    @Column(name = "level_code", nullable = false, length = 50)
    private String levelCode;

    @Column(name = "level_name", nullable = false, length = 100)
    private String levelName;

    @Column(name = "monthly_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRate;

    @Column(name = "required_quantity", nullable = false)
    private Integer requiredQuantity;

    @Column(name = "duration_in_months", nullable = false)
    private Integer durationInMonths;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost;

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (designationName != null) {
            designationName = designationName.trim();
        }
        if (levelCode != null) {
            levelCode = levelCode.trim().toUpperCase();
        }
        if (levelName != null) {
            levelName = levelName.trim();
        }
    }
}
