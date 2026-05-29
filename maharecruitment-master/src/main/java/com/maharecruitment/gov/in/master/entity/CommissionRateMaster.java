package com.maharecruitment.gov.in.master.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "commission_rate_master",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_commission_code_effective_date", columnNames = {
                        "commission_code", "effective_date" })
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRateMaster extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commission_rate_id")
    private Long commissionRateId;

    @NotNull(message = "Commission code is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "commission_code", nullable = false, length = 20)
    private CommissionCode commissionCode;

    @NotNull(message = "Commission percentage is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Commission percentage must be greater than zero")
    @DecimalMax(value = "100.0", inclusive = true, message = "Commission percentage cannot exceed 100")
    @Column(name = "commission_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    @NotNull(message = "Effective date is required")
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "active_flag", nullable = false, length = 1)
    @Builder.Default
    private String activeFlag = "Y";

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        activeFlag = (activeFlag == null || !"N".equalsIgnoreCase(activeFlag.trim())) ? "Y" : "N";
        if (commissionCode != null) {
            commissionCode = CommissionCode.valueOf(commissionCode.name().trim().toUpperCase(Locale.ROOT));
        }
    }
}
