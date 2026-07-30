package com.maharecruitment.gov.in.web.dto.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminApprovedPaymentReportFilterForm {

    private Long departmentRegistrationId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private String financialYear;

    private String searchTerm;
}
