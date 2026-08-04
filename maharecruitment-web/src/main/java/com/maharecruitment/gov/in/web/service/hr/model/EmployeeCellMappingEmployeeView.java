package com.maharecruitment.gov.in.web.service.hr.model;

import java.time.LocalDate;

public record EmployeeCellMappingEmployeeView(
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
        EmployeeCellOptionView mappedCell) {

    public boolean mapped() {
        return mappedCell != null;
    }
}
