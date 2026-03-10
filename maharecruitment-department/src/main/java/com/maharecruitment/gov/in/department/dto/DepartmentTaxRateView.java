package com.maharecruitment.gov.in.department.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentTaxRateView {

    private String taxCode;
    private String taxName;
    private BigDecimal ratePercentage;
}
