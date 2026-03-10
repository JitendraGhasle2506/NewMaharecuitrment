package com.maharecruitment.gov.in.department.service.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditorApplicationTaxComponentView {

    private String taxCode;
    private String taxName;
    private BigDecimal ratePercentage;
    private BigDecimal taxAmount;
}
