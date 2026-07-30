package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.attendance.repository.AttendanceRegisterRepo;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentDesignationVacancyRepository;
import com.maharecruitment.gov.in.web.service.dashboard.MDDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.CellProjectWorkforceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.MDDashboardView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MDDashboardServiceImpl implements MDDashboardService {

    private static final String INTERNAL = "INTERNAL";
    private static final String EXTERNAL = "EXTERNAL";
    private static final String PRESENT = "PRESENT";
    private static final String UNASSIGNED_CELL = "Unassigned Cell";

    private final ProjectMstRepository projectMstRepository;
    private final EmployeeRepository employeeRepository;
    private final RecruitmentDesignationVacancyRepository recruitmentDesignationVacancyRepository;
    private final DepartmentProjectApplicationRepository departmentProjectApplicationRepository;
    private final DailyAttendanceInternalRepository dailyAttendanceInternalRepository;
    private final AttendanceRegisterRepo attendanceRegisterRepo;

    @Override
    public MDDashboardView getDashboard() {
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

        long pendingApprovals = departmentProjectApplicationRepository
                .countByApplicationStatus(DepartmentApplicationStatus.SUBMITTED_TO_HR);

        long openPositions = recruitmentDesignationVacancyRepository.countTotalOpenPositions();

        return new MDDashboardView(
                toInt(totalProjects),
                toInt(internalProjects),
                toInt(externalProjects),
                toInt(onboardingThisMonth),
                toInt(internalEmployees),
                toInt(externalEmployees),
                toInt(totalEmployees),
                toInt(presentEmployees),
                toInt(absentEmployees),
                percent(presentEmployees, totalEmployees),
                toInt(pendingApprovals),
                toInt(openPositions),
                percent(internalEmployees, totalEmployees),
                percent(externalEmployees, totalEmployees),
                buildCellProjectSummary());
    }

    private long countPresentEmployees(LocalDate date) {
        return dailyAttendanceInternalRepository.countByAttendanceDateAndStatusIgnoreCase(date, PRESENT)
                + attendanceRegisterRepo.countExternalPresentByMonthYearDay(
                        date.getMonthValue(),
                        date.getYear(),
                        date.getDayOfMonth());
    }

    private List<CellProjectWorkforceView> buildCellProjectSummary() {
        Map<String, CellProjectAggregate> aggregates = new LinkedHashMap<>();

        projectMstRepository.summarizeProjectsByCell(ProjectScopeType.INTERNAL, ProjectScopeType.EXTERNAL, UNASSIGNED_CELL)
                .forEach(summary -> {
                    CellProjectAggregate aggregate = aggregates.computeIfAbsent(
                            normalizeCellName(summary.getCellName()),
                            ignored -> new CellProjectAggregate());
                    aggregate.totalProjects = toInt(summary.getTotalProjects());
                    aggregate.internalProjects = toInt(summary.getInternalProjects());
                    aggregate.externalProjects = toInt(summary.getExternalProjects());
                });

        departmentProjectApplicationRepository
                .summarizeEmployeeCountsByProjectCell(INTERNAL, EXTERNAL, UNASSIGNED_CELL)
                .forEach(summary -> {
                    CellProjectAggregate aggregate = aggregates.computeIfAbsent(
                            normalizeCellName(summary.getCellName()),
                            ignored -> new CellProjectAggregate());
                    aggregate.internalEmployees = toInt(summary.getInternalEmployees());
                    aggregate.externalEmployees = toInt(summary.getExternalEmployees());
                });

        return aggregates.entrySet()
                .stream()
                .map(entry -> new CellProjectWorkforceView(
                        entry.getKey(),
                        entry.getValue().totalProjects,
                        entry.getValue().internalProjects,
                        entry.getValue().externalProjects,
                        entry.getValue().internalEmployees,
                        entry.getValue().externalEmployees))
                .sorted(Comparator.comparing(CellProjectWorkforceView::cellName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private String normalizeCellName(String cellName) {
        return cellName != null && !cellName.isBlank() ? cellName.trim() : UNASSIGNED_CELL;
    }

    private int percent(long value, long total) {
        return total > 0 ? (int) ((value * 100) / total) : 0;
    }

    private int toInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static final class CellProjectAggregate {
        private int totalProjects;
        private int internalProjects;
        private int externalProjects;
        private int internalEmployees;
        private int externalEmployees;
    }
}
