package com.maharecruitment.gov.in.department.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedPaymentReportRowView {

    private Long paymentId;
    private Long applicationId;
    private String requestId;
    private String projectName;
    private String projectCode;
    private Long departmentRegistrationId;
    private String departmentName;
    private String workOrderNumber;
    private String proformaInvoiceId;
    private String receiptNumber;
    private String utrNumber;
    private String paymentMode;
    private BigDecimal totalAmount;
    private DepartmentApplicationStatus applicationStatus;
    private LocalDateTime createdDate;
    private LocalDateTime approvedDate;
    private String approvedBy;
    private String remarks;
}
