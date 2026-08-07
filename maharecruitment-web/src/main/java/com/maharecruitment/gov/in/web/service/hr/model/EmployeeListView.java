package com.maharecruitment.gov.in.web.service.hr.model;

import java.time.LocalDate;

public record EmployeeListView(
        Long employeeId,
        String employeeCode,
        String fullName,
        String email,
        String designation,
        LocalDate mahaitJoiningDate,
        String recruitmentType,
        String agencyName,
        String status) {
}
