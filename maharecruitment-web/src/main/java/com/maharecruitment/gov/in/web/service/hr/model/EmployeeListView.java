package com.maharecruitment.gov.in.web.service.hr.model;

import java.time.LocalDate;

public record EmployeeListView(
        Long employeeId,
        String employeeCode,
        String requestId,
        String projectName,
        String fullName,
        String email,
        String mobile,
        String designation,
        String department,
        LocalDate joiningDate,
        String recruitmentType,
        String agencyName,
        String status) {
}
