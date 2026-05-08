package com.maharecruitment.gov.in.department.service.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApprovedPaymentReportSummaryView {

    private long totalPayments;
    private BigDecimal totalApprovedAmount;
    private long totalDepartments;

    public ApprovedPaymentReportSummaryView(Number totalPayments, Number totalApprovedAmount, Number totalDepartments) {
        this.totalPayments = totalPayments == null ? 0L : totalPayments.longValue();
        this.totalApprovedAmount = totalApprovedAmount == null
                ? BigDecimal.ZERO
                : new BigDecimal(totalApprovedAmount.toString());
        this.totalDepartments = totalDepartments == null ? 0L : totalDepartments.longValue();
    }
}
