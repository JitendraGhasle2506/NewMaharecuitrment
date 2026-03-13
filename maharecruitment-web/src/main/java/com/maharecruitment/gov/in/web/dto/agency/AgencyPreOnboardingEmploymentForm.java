package com.maharecruitment.gov.in.web.dto.agency;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyPreOnboardingEmploymentForm {

    private Long preOnboardingEmploymentId;

    private String companyName;

    private String designation;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
