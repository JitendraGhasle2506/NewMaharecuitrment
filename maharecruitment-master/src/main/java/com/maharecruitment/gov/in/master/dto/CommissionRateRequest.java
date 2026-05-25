package com.maharecruitment.gov.in.master.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.maharecruitment.gov.in.master.entity.CommissionCode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommissionRateRequest {

    @NotNull(message = "Commission code is required")
    private CommissionCode commissionCode;

    @NotNull(message = "Commission percentage is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Commission percentage must be greater than zero")
    @DecimalMax(value = "100.0", inclusive = true, message = "Commission percentage cannot exceed 100")
    private BigDecimal commissionPercentage;

    @NotNull(message = "Effective date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveDate;

    private String activeFlag;
}
