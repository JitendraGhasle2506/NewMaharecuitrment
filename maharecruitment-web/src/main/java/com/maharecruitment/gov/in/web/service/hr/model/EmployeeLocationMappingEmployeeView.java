package com.maharecruitment.gov.in.web.service.hr.model;

import java.time.LocalDate;
import java.util.List;

public record EmployeeLocationMappingEmployeeView(
        Long employeeId,
        String employeeCode,
        String fullName,
        String email,
        String mobile,
        String designation,
        String department,
        String projectName,
        String recruitmentType,
        LocalDate joiningDate,
        LocalDate onboardingDate,
        List<EmployeeLocationOptionView> mappedLocations) {
}
