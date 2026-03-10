package com.maharecruitment.gov.in.department.service.model;

import java.time.LocalDate;
import java.util.List;

public record DepartmentDashboardView(
        String departmentTitle,
        int registeredProjectCount,
        int employeeCount,
        int runningProjectCount,
        List<DepartmentRunningProjectView> runningProjects,
        LocalDate snapshotDate) {
}
