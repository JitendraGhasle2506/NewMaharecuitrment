package com.maharecruitment.gov.in.master.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "manpower_designation_rate",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "designation_id", "level_code", "effective_from" })
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManpowerDesignationRate extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rateId;

    @NotNull(message = "Designation id is required")
    @Column(name = "designation_id", nullable = false)
    private Long designationId;

    @NotBlank(message = "Level code is required")
    @Column(name = "level_code", nullable = false, length = 50)
    private String levelCode;

    @NotNull(message = "Gross monthly CTC is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gross monthly CTC must be greater than zero")
    @Column(name = "gross_monthly_ctc", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossMonthlyCtc;

    @NotNull(message = "Effective from date is required")
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active_flag", length = 1, nullable = false)
    @Builder.Default
    private String activeFlag = "Y";

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (levelCode != null) {
            levelCode = levelCode.trim().toUpperCase();
        }
        activeFlag = (activeFlag == null || !"N".equalsIgnoreCase(activeFlag)) ? "Y" : "N";
    }
}

