package com.maharecruitment.gov.in.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyMonthlyBillListItemView {

    private Long agencyMonthlyBillId;
    private String billNumber;
    private Long agencyId;
    private String agencyName;
    private Integer billYear;
    private Integer billMonth;
    private String employeeType;
    private LocalDate generatedDate;
    private Integer employeeCount;
    private BigDecimal attendanceAmount;
    private BigDecimal agencyMarginAmount;
    private BigDecimal totalAmount;
}
