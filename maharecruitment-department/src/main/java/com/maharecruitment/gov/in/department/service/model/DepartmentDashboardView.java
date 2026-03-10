package com.maharecruitment.gov.in.department.service.model;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DepartmentDashboardView {

    private String departmentTitle;
    private int registeredProjectCount;
    private int employeeCount;
    private int runningProjectCount;
    private List<DepartmentRunningProjectView> runningProjects;
    private LocalDate snapshotDate;
}
