package com.maharecruitment.gov.in.master.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ManpowerDesignationRateResponse {

    private Long rateId;
    private Long designationId;
    private String levelCode;
    private BigDecimal grossMonthlyCtc;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String activeFlag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

