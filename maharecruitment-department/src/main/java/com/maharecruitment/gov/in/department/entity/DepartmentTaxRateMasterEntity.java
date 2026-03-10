package com.maharecruitment.gov.in.department.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "department_tax_rate_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentTaxRateMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_tax_rate_master_id")
    private Long departmentTaxRateMasterId;

    @Column(name = "tax_code", nullable = false, length = 40)
    private String taxCode;

    @Column(name = "tax_name", length = 120)
    private String taxName;

    @Column(name = "rate_percentage", nullable = false, precision = 8, scale = 4)
    private BigDecimal ratePercentage;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (taxCode != null) {
            taxCode = taxCode.trim().toUpperCase(Locale.ROOT);
        }
        if (taxName != null) {
            taxName = taxName.trim();
        }
        active = Boolean.TRUE.equals(active);
    }
}
