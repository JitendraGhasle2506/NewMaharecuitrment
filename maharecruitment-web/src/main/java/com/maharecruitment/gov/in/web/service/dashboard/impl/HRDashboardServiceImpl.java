package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.attendance.repository.AttendanceRegisterRepo;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectCellCountProjection;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.repository.WingMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.PositionCellEmployeeCountProjection;
import com.maharecruitment.gov.in.recruitment.repository.organization.PositionMasterRepository;
import com.maharecruitment.gov.in.web.service.dashboard.HRDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRCellReportView;
import com.maharecruitment.gov.in.web.service.dashboard.model.DepartmentOnboardingView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;
import com.maharecruitment.gov.in.web.service.dashboard.model.ProjectScopeListItemView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HRDashboardServiceImpl implements HRDashboardService {

    private static final String ACTIVE_FLAG = "Y";
    private static final String ACTIVE_EMPLOYEE_STATUS = "ACTIVE";
    private static final String INTERNAL = "INTERNAL";
    private static final String EXTERNAL = "EXTERNAL";
    private static final String PRESENT = "PRESENT";

    private final ProjectMstRepository projectMstRepository;
    private final WingMasterRepository wingMasterRepository;
    private final CellMasterRepository cellMasterRepository;
    private final EmployeeRepository employeeRepository;
    private final PositionMasterRepository positionMasterRepository;
    private final DepartmentProjectApplicationRepository departmentProjectApplicationRepository;
    private final DailyAttendanceInternalRepository dailyAttendanceInternalRepository;
    private final AttendanceRegisterRepo attendanceRegisterRepo;

    @Override
    @Transactional(readOnly = true)
    public HRDashboardView getDashboard() {
        List<ProjectScopeListItemView> internalProjectList = buildProjectList(ProjectScopeType.INTERNAL);
        List<ProjectScopeListItemView> externalProjectList = buildProjectList(ProjectScopeType.EXTERNAL);
        List<HRWingReportView> wingReports = buildWingReports();
        int internalProjects = internalProjectList.size();
        int externalProjects = externalProjectList.size();
        int totalProjects = internalProjects + externalProjects;
        int totalCells = wingReports.stream().mapToInt(HRWingReportView::cellCount).sum();
        int wingProjectCount = wingReports.stream().mapToInt(HRWingReportView::projectCount).sum();
        int wingEmployeeCount = wingReports.stream().mapToInt(HRWingReportView::employeeCount).sum();
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

        int internalPercent = totalEmployees > 0 ? (int) ((internalEmployees * 100) / totalEmployees) : 0;
        int externalPercent = totalEmployees > 0 ? (int) ((externalEmployees * 100) / totalEmployees) : 0;
        int presentPercent = totalEmployees > 0 ? (int) ((presentEmployees * 100) / totalEmployees) : 0;

        List<EmployeeEntity> allEmployees = employeeRepository.findAll();
        Map<String, Long> deptCounts = allEmployees.stream()
                .filter(employee -> employee.getDepartmentRegistration() != null
                        && hasText(employee.getDepartmentRegistration().getDepartmentName()))
                .collect(Collectors.groupingBy(
                        employee -> employee.getDepartmentRegistration().getDepartmentName().trim(),
                        Collectors.counting()));

        List<DepartmentOnboardingView> departmentOnboarding = deptCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> {
                    int count = toInt(entry.getValue());
                    return new DepartmentOnboardingView(entry.getKey(), count, count + 10);
                })
                .limit(5)
                .collect(Collectors.toList());

        if (departmentOnboarding.isEmpty()) {
            departmentOnboarding = List.of(new DepartmentOnboardingView("No Data", 0, 0));
        }

        return new HRDashboardView(
                totalProjects,
                internalProjects,
                externalProjects,
                (int) onboardingThisMonth,
                (int) internalEmployees,
                (int) externalEmployees,
                (int) totalEmployees,
                (int) presentEmployees,
                (int) absentEmployees,
                presentPercent,
                (int) pendingApprovals,
                wingReports.size(),
                totalCells,
                wingProjectCount,
                wingEmployeeCount,
                internalPercent,
                externalPercent,
                departmentOnboarding,
                internalProjectList,
                externalProjectList,
                wingReports
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HRWingReportView> getWingReport(Long wingId) {
        if (wingId == null) {
            return Optional.empty();
        }

        return wingMasterRepository.findByWingIdAndActiveFlagIgnoreCase(wingId, ACTIVE_FLAG)
                .map(wing -> {
                    List<CellMaster> cells = cellMasterRepository
                            .findByWing_WingIdAndActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(
                                    wingId,
                                    ACTIVE_FLAG,
                                    ACTIVE_FLAG);
                    Map<Long, Integer> projectCountsByCellId = buildProjectCountsByCellId(wingId);
                    Map<Long, Integer> employeeCountsByCellId = buildEmployeeCountsByCellId(wingId);

                    return toWingReportView(
                            wing,
                            cells,
                            projectCountsByCellId,
                            employeeCountsByCellId,
                            maxCount(cells, projectCountsByCellId),
                            maxCount(cells, employeeCountsByCellId));
                });
    }

    private List<HRWingReportView> buildWingReports() {
        List<WingMaster> wings = wingMasterRepository.findByActiveFlagIgnoreCaseOrderByWingNameAsc(ACTIVE_FLAG);
        List<CellMaster> cells = cellMasterRepository
                .findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(ACTIVE_FLAG, ACTIVE_FLAG);
        Map<Long, List<CellMaster>> cellsByWingId = cells.stream()
                .filter(cell -> cell.getWing() != null && cell.getWing().getWingId() != null)
                .collect(Collectors.groupingBy(
                        cell -> cell.getWing().getWingId(),
                        Collectors.toList()));
        Map<Long, Integer> projectCountsByCellId = buildProjectCountsByCellId();
        Map<Long, Integer> employeeCountsByCellId = buildEmployeeCountsByCellId();
        int maxProjectCount = maxCount(cells, projectCountsByCellId);
        int maxEmployeeCount = maxCount(cells, employeeCountsByCellId);

        return wings.stream()
                .map(wing -> toWingReportView(
                        wing,
                        cellsByWingId.getOrDefault(wing.getWingId(), List.of()),
                        projectCountsByCellId,
                        employeeCountsByCellId,
                        maxProjectCount,
                        maxEmployeeCount))
                .toList();
    }

    private Map<Long, Integer> buildProjectCountsByCellId() {
        return toProjectCountMap(projectMstRepository.summarizeProjectCountsByCell());
    }

    private Map<Long, Integer> buildProjectCountsByCellId(Long wingId) {
        return toProjectCountMap(projectMstRepository.summarizeProjectCountsByCellAndWingId(wingId));
    }

    private Map<Long, Integer> toProjectCountMap(List<ProjectCellCountProjection> summaries) {
        return summaries.stream()
                .filter(summary -> summary.getCellId() != null)
                .collect(Collectors.toMap(
                        ProjectCellCountProjection::getCellId,
                        summary -> toInt(summary.getProjectCount()),
                        Integer::sum));
    }

    private Map<Long, Integer> buildEmployeeCountsByCellId() {
        return toEmployeeCountMap(positionMasterRepository.summarizeFilledActiveEmployeesByCell(
                        OrganizationRecordStatus.ACTIVE,
                        PositionStatus.FILLED,
                        ACTIVE_EMPLOYEE_STATUS));
    }

    private Map<Long, Integer> buildEmployeeCountsByCellId(Long wingId) {
        return toEmployeeCountMap(positionMasterRepository.summarizeFilledActiveEmployeesByCellAndWingId(
                        wingId,
                        OrganizationRecordStatus.ACTIVE,
                        PositionStatus.FILLED,
                        ACTIVE_EMPLOYEE_STATUS));
    }

    private Map<Long, Integer> toEmployeeCountMap(List<PositionCellEmployeeCountProjection> summaries) {
        return summaries.stream()
                .filter(summary -> summary.getCellId() != null)
                .collect(Collectors.toMap(
                        PositionCellEmployeeCountProjection::getCellId,
                        summary -> toInt(summary.getEmployeeCount()),
                        Integer::sum));
    }

    private HRWingReportView toWingReportView(
            WingMaster wing,
            List<CellMaster> cells,
            Map<Long, Integer> projectCountsByCellId,
            Map<Long, Integer> employeeCountsByCellId,
            int maxProjectCount,
            int maxEmployeeCount) {
        List<HRCellReportView> cellReports = cells.stream()
                .sorted(Comparator.comparing(
                        cell -> defaultText(cell.getCellName(), ""),
                        String.CASE_INSENSITIVE_ORDER))
                .map(cell -> toCellReportView(
                        cell,
                        projectCountsByCellId.getOrDefault(cell.getCellId(), 0),
                        employeeCountsByCellId.getOrDefault(cell.getCellId(), 0),
                        maxProjectCount,
                        maxEmployeeCount))
                .toList();
        int projectCount = cellReports.stream().mapToInt(HRCellReportView::projectCount).sum();
        int employeeCount = cellReports.stream().mapToInt(HRCellReportView::employeeCount).sum();
        return new HRWingReportView(
                wing.getWingId(),
                defaultText(wing.getWingName(), "Unnamed Wing"),
                cellReports.size(),
                projectCount,
                employeeCount,
                cellReports);
    }

    private HRCellReportView toCellReportView(
            CellMaster cell,
            int projectCount,
            int employeeCount,
            int maxProjectCount,
            int maxEmployeeCount) {
        return new HRCellReportView(
                cell.getCellId(),
                defaultText(cell.getCellName(), "Unnamed Cell"),
                projectCount,
                employeeCount,
                percent(projectCount, maxProjectCount),
                percent(employeeCount, maxEmployeeCount));
    }

    private List<ProjectScopeListItemView> buildProjectList(ProjectScopeType projectScopeType) {
        return projectMstRepository.findByProjectScopeTypeOrderByProjectNameAsc(projectScopeType)
                .stream()
                .map(project -> new ProjectScopeListItemView(
                        normalizeCode(project.getProjectCode()),
                        normalizeProjectName(project.getProjectName())))
                .sorted(Comparator.comparing(ProjectScopeListItemView::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ProjectScopeListItemView::code, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private String normalizeCode(String code) {
        return code != null && !code.isBlank() ? code.trim() : null;
    }

    private String normalizeProjectName(String name) {
        return name != null && !name.isBlank() ? name.trim() : "Unnamed Project";
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int maxCount(List<CellMaster> cells, Map<Long, Integer> countsByCellId) {
        return cells.stream()
                .filter(cell -> cell.getCellId() != null)
                .mapToInt(cell -> countsByCellId.getOrDefault(cell.getCellId(), 0))
                .max()
                .orElse(0);
    }

    private int percent(int value, int total) {
        return total > 0 ? (value * 100) / total : 0;
    }

    private int toInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private long countPresentEmployees(LocalDate today) {
        return dailyAttendanceInternalRepository.countByAttendanceDateAndStatusIgnoreCase(today, PRESENT)
                + attendanceRegisterRepo.countExternalPresentByMonthYearDay(
                        today.getMonthValue(),
                        today.getYear(),
                        today.getDayOfMonth());
    }
}
