package com.maharecruitment.gov.in.department.service.model;

import java.time.LocalDate;

public record DepartmentRunningProjectView(
        String projectCode,
        String projectName,
        LocalDate startDate,
        int allocatedEmployees,
        String status) {
}
