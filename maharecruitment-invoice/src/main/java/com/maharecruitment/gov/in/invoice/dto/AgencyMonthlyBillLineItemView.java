package com.maharecruitment.gov.in.invoice.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyMonthlyBillLineItemView {

    private Integer lineNumber;
    private Long employeeId;
    private String employeeCode;
    private String requestId;
    private String employeeName;
    private String employeeType;
    private Long designationId;
    private String designationName;
    private String levelCode;
    private BigDecimal monthlyRate;
    private Integer daysInMonth;
    private Long payableDays;
    private Long presentDays;
    private Long absentDays;
    private Long leaveDays;
    private Long compOffDays;
    private Long tourDays;
    private Long holidayDays;
    private Long weekOffDays;
    private BigDecimal attendanceAmount;
    private BigDecimal agencyMarginRate;
    private BigDecimal agencyMarginAmount;
    private BigDecimal lineTotal;
}
