package com.maharecruitment.gov.in.invoice.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaxInvoiceGenerationApplicationView {

    private final Long applicationId;
    private final Long departmentTaxInvoiceId;
    private final String requestId;
    private final String tiNumber;
    private final LocalDate issueDate;
    private final String projectName;
    private final String projectCode;
    private final String departmentName;
    private final String subDepartmentName;
    private final String generationStatus;
    private final String message;

    public boolean isGenerated() {
        return departmentTaxInvoiceId != null;
    }

    public String getInvoiceUrl() {
        return applicationId == null ? "#" : "/invoice/tax-invoices/application/" + applicationId;
    }
}
