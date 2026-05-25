package com.maharecruitment.gov.in.master.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.maharecruitment.gov.in.master.entity.CommissionCode;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommissionRateResponse {

    private Long commissionRateId;
    private CommissionCode commissionCode;
    private BigDecimal commissionPercentage;
    private LocalDate effectiveDate;
    private String activeFlag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
