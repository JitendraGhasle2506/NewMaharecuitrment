package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentDesignationVacancyRepository;
import com.maharecruitment.gov.in.web.service.dashboard.HRDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.DepartmentOnboardingView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.ProjectWorkforceView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HRDashboardServiceImpl implements HRDashboardService {

    private final ProjectMstRepository projectMstRepository;
    private final EmployeeRepository employeeRepository;
    private final RecruitmentDesignationVacancyRepository recruitmentDesignationVacancyRepository;
    private final DepartmentProjectApplicationRepository departmentProjectApplicationRepository;

    @Override
    public HRDashboardView getDashboard() {
        // Basic Counts
        long totalProjects = projectMstRepository.count();
        long internalEmployees = employeeRepository.countByRecruitmentType("INTERNAL");
        long externalEmployees = employeeRepository.countByRecruitmentType("EXTERNAL");
        long totalEmployees = employeeRepository.count();

        // Monthly Onboarding
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate lastDayOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        long onboardingThisMonth = employeeRepository.countByOnboardingDateBetween(firstDayOfMonth, lastDayOfMonth);

        // Pending Approvals (Applications in SUBMITTED status)
        long pendingApprovals = departmentProjectApplicationRepository.findAll().stream()
                .filter(app -> app.getApplicationStatus() == DepartmentApplicationStatus.SUBMITTED_TO_HR)
                .count();

        // Open Positions
        long openPositions = recruitmentDesignationVacancyRepository.countTotalOpenPositions();

        // Percentages
        int internalPercent = totalEmployees > 0 ? (int) ((internalEmployees * 100) / totalEmployees) : 0;
        int externalPercent = totalEmployees > 0 ? (int) ((externalEmployees * 100) / totalEmployees) : 0;

        // Department Onboarding
        List<EmployeeEntity> allEmployees = employeeRepository.findAll();
        Map<String, Long> deptCounts = allEmployees.stream()
                .filter(e -> e.getDepartmentRegistration() != null)
                .collect(Collectors.groupingBy(e -> e.getDepartmentRegistration().getDepartmentName(), Collectors.counting()));

        List<DepartmentOnboardingView> departmentOnboarding = deptCounts.entrySet().stream()
                .map(entry -> new DepartmentOnboardingView(entry.getKey(), entry.getValue().intValue(), entry.getValue().intValue() + 10)) // target is dummy for now
                .limit(5)
                .collect(Collectors.toList());

        if (departmentOnboarding.isEmpty()) {
            departmentOnboarding = List.of(new DepartmentOnboardingView("No Data", 0, 0));
        }

        // Project Workforce Snapshot
        List<DepartmentProjectApplicationEntity> applications = departmentProjectApplicationRepository.findAll();
        List<ProjectWorkforceView> projects = applications.stream()
                .map(app -> {
                    long internal = allEmployees.stream()
                            .filter(e -> app.getRequestId().equals(e.getRequestId()) && "INTERNAL".equals(e.getRecruitmentType()))
                            .count();
                    long external = allEmployees.stream()
                            .filter(e -> app.getRequestId().equals(e.getRequestId()) && "EXTERNAL".equals(e.getRecruitmentType()))
                            .count();
                    return new ProjectWorkforceView(app.getProjectCode(), app.getProjectName(), (int) internal, (int) external, app.getApplicationStatus().toString());
                })
                .limit(5)
                .collect(Collectors.toList());

        if (projects.isEmpty()) {
            projects = Collections.emptyList();
        }

        return new HRDashboardView(
                (int) totalProjects,
                (int) onboardingThisMonth,
                (int) internalEmployees,
                (int) externalEmployees,
                (int) totalEmployees,
                (int) pendingApprovals,
                (int) openPositions,
                "0.0%", // Attrition rate dummy for now
                internalPercent,
                externalPercent,
                departmentOnboarding,
                projects
        );
    }
}
