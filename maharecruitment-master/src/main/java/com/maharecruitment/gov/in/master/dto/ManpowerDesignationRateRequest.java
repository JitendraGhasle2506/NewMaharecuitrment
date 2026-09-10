package com.maharecruitment.gov.in.master.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManpowerDesignationRateRequest {

    @NotNull(message = "Designation id is required")
    private Long designationId;

    @NotBlank(message = "Level code is required")
    private String levelCode;

    @NotNull(message = "Gross monthly CTC is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gross monthly CTC must be greater than zero")
    private BigDecimal grossMonthlyCtc;

    @NotNull(message = "Effective from date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveTo;
    private String activeFlag;
}
