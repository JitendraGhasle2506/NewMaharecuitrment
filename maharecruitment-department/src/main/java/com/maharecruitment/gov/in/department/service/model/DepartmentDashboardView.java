package com.maharecruitment.gov.in.department.service.model;

import java.time.LocalDate;
import java.util.List;

import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DepartmentDashboardView {

    private String departmentTitle;
    private String subDepartmentName;
    private int registeredProjectCount;
    private int employeeCount;
    private int runningProjectCount;
    private int pendingPaymentCount;
    private List<DepartmentRunningProjectView> runningProjects;
    private LocalDate snapshotDate;
    List<EmployeeEntity> employees; 
    List<DepartmentProjectApplicationEntity> allProjects; 
    List<DepartmentProjectApplicationEntity> runningProjectsEntities; 
}
