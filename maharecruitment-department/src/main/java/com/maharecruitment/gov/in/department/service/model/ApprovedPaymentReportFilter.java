package com.maharecruitment.gov.in.department.service.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedPaymentReportFilter {

    private Long departmentRegistrationId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String financialYear;
    private String searchTerm;
}
