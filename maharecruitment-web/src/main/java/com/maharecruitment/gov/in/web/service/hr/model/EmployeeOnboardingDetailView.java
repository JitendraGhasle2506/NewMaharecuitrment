package com.maharecruitment.gov.in.web.service.hr.model;

import java.time.LocalDate;

import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;

public record EmployeeOnboardingDetailView(
        Long employeeId,
        String employeeCode,
        String status,
        String recruitmentType,
        LocalDate resignationDate,
        AgencyPreOnboardingForm onboardingForm) {
}
