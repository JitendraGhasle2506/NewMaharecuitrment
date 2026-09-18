package com.maharecruitment.gov.in.web.service.hr.model;

public record EmployeeProjectMappingEmployeeView(
        Long employeeId,
        String fullName,
        String email,
        String designation,
        String department,
        String recruitmentType,
        EmployeeProjectOptionView mappedProject) {

    public boolean mapped() {
        return mappedProject != null;
    }
}
