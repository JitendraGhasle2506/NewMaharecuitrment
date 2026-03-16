package com.maharecruitment.gov.in.web.service.agency.model;

import java.time.LocalDate;

public record AgencyOnboardedEmployeeView(
        Long employeeId,
        String employeeCode,
        String requestId,
        String projectName,
        String departmentName,
        String subDepartmentName,
        String candidateName,
        String candidateEmail,
        String candidateMobile,
        String designationName,
        String levelCode,
        LocalDate joiningDate,
        LocalDate onboardingDate,
        LocalDate resignationDate,
        String status) {
}
