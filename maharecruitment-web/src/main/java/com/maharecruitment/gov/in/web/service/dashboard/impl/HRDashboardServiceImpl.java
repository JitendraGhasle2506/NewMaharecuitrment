package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.attendance.repository.AttendanceRegisterRepo;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
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

    private static final String INTERNAL = "INTERNAL";
    private static final String EXTERNAL = "EXTERNAL";
    private static final String PRESENT = "PRESENT";
    private static final String UNASSIGNED_CELL = "Unassigned Cell";
    private static final int PROJECT_WORKFORCE_SNAPSHOT_LIMIT = 10;

    private final ProjectMstRepository projectMstRepository;
    private final EmployeeRepository employeeRepository;
    private final RecruitmentDesignationVacancyRepository recruitmentDesignationVacancyRepository;
    private final DepartmentProjectApplicationRepository departmentProjectApplicationRepository;
    private final DailyAttendanceInternalRepository dailyAttendanceInternalRepository;
    private final AttendanceRegisterRepo attendanceRegisterRepo;

    @Override
    public HRDashboardView getDashboard() {
        long totalProjects = projectMstRepository.count();
        long internalProjects = projectMstRepository.countByProjectScopeType(ProjectScopeType.INTERNAL);
        long externalProjects = projectMstRepository.countByProjectScopeType(ProjectScopeType.EXTERNAL);
        long internalEmployees = employeeRepository.countByRecruitmentType(INTERNAL);
        long externalEmployees = employeeRepository.countByRecruitmentType(EXTERNAL);
        long totalEmployees = employeeRepository.count();

        LocalDate today = LocalDate.now();
        long presentEmployees = countPresentEmployees(today);
        long absentEmployees = Math.max(totalEmployees - presentEmployees, 0);

        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        long onboardingThisMonth = employeeRepository.countByOnboardingDateBetween(firstDayOfMonth, lastDayOfMonth);

        long pendingApprovals = departmentProjectApplicationRepository.countByApplicationStatus(
                DepartmentApplicationStatus.SUBMITTED_TO_HR);
        long openPositions = recruitmentDesignationVacancyRepository.countTotalOpenPositions();

        int internalPercent = totalEmployees > 0 ? (int) ((internalEmployees * 100) / totalEmployees) : 0;
        int externalPercent = totalEmployees > 0 ? (int) ((externalEmployees * 100) / totalEmployees) : 0;
        int presentPercent = totalEmployees > 0 ? (int) ((presentEmployees * 100) / totalEmployees) : 0;

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

        List<ProjectMst> masterProjects = projectMstRepository.findAll();
        Map<Long, DepartmentProjectApplicationEntity> applicationsById = departmentProjectApplicationRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        DepartmentProjectApplicationEntity::getDepartmentProjectApplicationId,
                        application -> application,
                        (first, second) -> first));
        Map<String, WorkforceCount> workforceByRequestId = buildWorkforceByRequestId(allEmployees);
        List<ProjectWorkforceView> projects = masterProjects.stream()
                .map(project -> {
                    DepartmentProjectApplicationEntity app = resolveApplication(project, applicationsById);
                    WorkforceCount workforce = app != null
                            ? workforceByRequestId.getOrDefault(app.getRequestId(), WorkforceCount.empty())
                            : WorkforceCount.empty();
                    return new ProjectWorkforceView(
                            app != null ? app.getProjectCode() : null,
                            project.getProjectName(),
                            resolveCellName(project),
                            workforce.internal(),
                            workforce.external(),
                            app != null ? app.getApplicationStatus().toString() : resolveProjectStatus(project));
                })
                .sorted(Comparator.comparing(ProjectWorkforceView::cellName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(ProjectWorkforceView::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(ProjectWorkforceView::code, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .limit(PROJECT_WORKFORCE_SNAPSHOT_LIMIT)
                .collect(Collectors.toList());

        if (projects.isEmpty()) {
            projects = Collections.emptyList();
        }

        return new HRDashboardView(
                (int) totalProjects,
                (int) internalProjects,
                (int) externalProjects,
                (int) onboardingThisMonth,
                (int) internalEmployees,
                (int) externalEmployees,
                (int) totalEmployees,
                (int) presentEmployees,
                (int) absentEmployees,
                presentPercent,
                (int) pendingApprovals,
                (int) openPositions,
                "0.0%", // Attrition rate dummy for now
                internalPercent,
                externalPercent,
                departmentOnboarding,
                projects
        );
    }

    private DepartmentProjectApplicationEntity resolveApplication(
            ProjectMst project,
            Map<Long, DepartmentProjectApplicationEntity> applicationsById) {
        return project != null && project.getApplicationId() != null
                ? applicationsById.get(project.getApplicationId())
                : null;
    }

    private String resolveCellName(ProjectMst project) {
        if (hasCellName(project)) {
            return project.getCell().getCellName().trim();
        }

        return UNASSIGNED_CELL;
    }

    private String resolveProjectStatus(ProjectMst project) {
        return project != null && "N".equalsIgnoreCase(project.getActiveFlag()) ? "Inactive" : "Active";
    }

    private boolean hasCellName(ProjectMst project) {
        return project != null
                && project.getCell() != null
                && hasText(project.getCell().getCellName());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private long countPresentEmployees(LocalDate today) {
        return dailyAttendanceInternalRepository.countByAttendanceDateAndStatusIgnoreCase(today, PRESENT)
                + attendanceRegisterRepo.countExternalPresentByMonthYearDay(
                        today.getMonthValue(),
                        today.getYear(),
                        today.getDayOfMonth());
    }

    private Map<String, WorkforceCount> buildWorkforceByRequestId(List<EmployeeEntity> employees) {
        Map<String, WorkforceCount> workforceByRequestId = new HashMap<>();
        for (EmployeeEntity employee : employees) {
            String requestId = employee.getRequestId();
            if (requestId == null || requestId.isBlank()) {
                continue;
            }
            WorkforceCount workforceCount = workforceByRequestId.computeIfAbsent(requestId, key -> new WorkforceCount());
            workforceCount.add(employee.getRecruitmentType());
        }
        return workforceByRequestId;
    }

    private static final class WorkforceCount {

        private int internal;
        private int external;

        private static WorkforceCount empty() {
            return new WorkforceCount();
        }

        private int internal() {
            return internal;
        }

        private int external() {
            return external;
        }

        private void add(String recruitmentType) {
            if (INTERNAL.equalsIgnoreCase(recruitmentType)) {
                internal++;
            } else if (EXTERNAL.equalsIgnoreCase(recruitmentType)) {
                external++;
            }
        }
    }
}
