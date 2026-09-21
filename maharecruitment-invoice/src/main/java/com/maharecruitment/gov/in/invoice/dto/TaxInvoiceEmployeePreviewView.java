package com.maharecruitment.gov.in.invoice.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaxInvoiceEmployeePreviewView {

    private final Long employeeId;
    private final String employeeCode;
    private final String fullName;
    private final String email;
    private final String designationName;
    private final String levelCode;
    private final String recruitmentType;
    private final String departmentName;
    private final String subDepartmentName;
    private final String projectName;
    private final LocalDate onboardingDate;
    private final LocalDate resignationDate;
    /** Days the employee was on the project within the selected invoice period (inclusive). */
    private final long numberOfDays;
}
