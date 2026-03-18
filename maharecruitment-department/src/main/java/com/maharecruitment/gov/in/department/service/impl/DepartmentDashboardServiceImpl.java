package com.maharecruitment.gov.in.department.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.service.DepartmentDashboardService;
import com.maharecruitment.gov.in.department.service.model.DepartmentDashboardView;
import com.maharecruitment.gov.in.department.service.model.DepartmentRunningProjectView;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
public class DepartmentDashboardServiceImpl implements DepartmentDashboardService {

    private final DepartmentProjectApplicationRepository projectApplicationRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentDashboardServiceImpl(
            DepartmentProjectApplicationRepository projectApplicationRepository,
            EmployeeRepository employeeRepository) {
        this.projectApplicationRepository = projectApplicationRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public DepartmentDashboardView getDashboard(Long departmentRegistrationId, String departmentDisplayName) {
        
        // Fetch all projects for the department
        List<DepartmentProjectApplicationEntity> allProjects = projectApplicationRepository
                .findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(departmentRegistrationId);
                
        int registeredProjectCount = allProjects.size();

        // Determine running projects based on status
        List<DepartmentProjectApplicationEntity> runningProjectsEntities = allProjects.stream()
                .filter(p -> p.getApplicationStatus() == DepartmentApplicationStatus.AUDITOR_APPROVED
                        || p.getApplicationStatus() == DepartmentApplicationStatus.COMPLETED)
                .collect(Collectors.toList());

        int runningProjectCount = runningProjectsEntities.size();

        // Fetch all employees for the department
        List<EmployeeEntity> employees = employeeRepository
                .findByDepartmentRegistration_DepartmentRegistrationId(departmentRegistrationId);
                
        int employeeCount = employees.size();

        // Format department title
        String title = StringUtils.hasText(departmentDisplayName)
                ? departmentDisplayName.trim() + " Department"
                : "Department";

        // Map running projects to view objects
        List<DepartmentRunningProjectView> runningProjects = runningProjectsEntities.stream()
                .map(p -> new DepartmentRunningProjectView(
                        p.getProjectCode() != null ? p.getProjectCode() : "N/A",
                        p.getProjectName(),
                        p.getCreatedDate() != null ? p.getCreatedDate().toLocalDate() : LocalDate.now(),
                        (int) employees.stream().filter(e -> Objects.equals(e.getRequestId(), p.getRequestId())).count(),
                        p.getApplicationStatus().getDisplayName()))
                .collect(Collectors.toList());

        return new DepartmentDashboardView(
                title,
                registeredProjectCount,
                employeeCount,
                runningProjectCount,
                runningProjects,
                LocalDate.now());
    }
}
