package com.maharecruitment.gov.in.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyMonthlyBillView {

    private Long agencyMonthlyBillId;
    private String billNumber;
    private Long agencyId;
    private String agencyName;
    private Integer billYear;
    private Integer billMonth;
    private String employeeType;
    private LocalDate generatedDate;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private Integer daysInMonth;
    private Integer employeeCount;
    private BigDecimal agencyMarginRate;
    private BigDecimal attendanceAmount;
    private BigDecimal agencyMarginAmount;
    private BigDecimal totalAmount;
    private LocalDateTime createdDate;
    private String createdBy;
    private List<AgencyMonthlyBillLineItemView> lineItems;
}
