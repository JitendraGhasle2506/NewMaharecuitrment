package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.attendance.repository.AttendanceCellSummaryProjection;
import com.maharecruitment.gov.in.attendance.repository.AttendanceCheckInSummaryProjection;
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
import com.maharecruitment.gov.in.recruitment.entity.CellReportingAuthorityMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.CellReportingAuthorityMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellCountProjection;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeDepartmentCountProjection;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.web.service.dashboard.HRDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.DepartmentOnboardingView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRAttendanceSummaryView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRCellAttendanceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRCellReportView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HREmployeeHierarchyView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRTodayAttendanceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingDirectoryItemView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportsView;
import com.maharecruitment.gov.in.web.service.dashboard.model.ProjectScopeListItemView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HRDashboardServiceImpl implements HRDashboardService {

    private static final String ACTIVE_FLAG = "Y";
    private static final String ACTIVE_EMPLOYEE_STATUS = "ACTIVE";
    private static final String INTERNAL = "INTERNAL";
    private static final String EXTERNAL = "EXTERNAL";
    private static final LocalTime EARLY_CHECK_IN_CUTOFF = LocalTime.of(9, 45);
    private static final LocalTime LATE_CHECK_IN_CUTOFF = LocalTime.of(10, 15);
    private static final String REPORTING_SOURCE_DIRECT = "DIRECT";
    private static final String REPORTING_SOURCE_CELL = "CELL";
    private static final String REPORTING_SOURCE_NONE = "NONE";

    private final ProjectMstRepository projectMstRepository;
    private final WingMasterRepository wingMasterRepository;
    private final CellMasterRepository cellMasterRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeCellMappingRepository employeeCellMappingRepository;
    private final EmployeeReportingMappingRepository employeeReportingMappingRepository;
    private final CellReportingAuthorityMappingRepository cellReportingAuthorityMappingRepository;
    private final DepartmentProjectApplicationRepository departmentProjectApplicationRepository;
    private final DailyAttendanceInternalRepository dailyAttendanceInternalRepository;
    private final AttendanceRegisterRepo attendanceRegisterRepo;

    @Override
    @Transactional(readOnly = true)
    public HRDashboardView getDashboard() {
        List<ProjectScopeListItemView> internalProjectList = buildProjectList(ProjectScopeType.INTERNAL);
        List<ProjectScopeListItemView> externalProjectList = buildProjectList(ProjectScopeType.EXTERNAL);
        int internalProjects = internalProjectList.size();
        int externalProjects = externalProjectList.size();
        int totalProjects = internalProjects + externalProjects;
        int totalWings = toInt(wingMasterRepository.countByActiveFlagIgnoreCase(ACTIVE_FLAG));
        int totalCells = toInt(cellMasterRepository
                .countByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCase(ACTIVE_FLAG, ACTIVE_FLAG));
        long internalEmployees = employeeRepository.countByRecruitmentType(INTERNAL);
        long externalEmployees = employeeRepository.countByRecruitmentType(EXTERNAL);
        long totalEmployees = employeeRepository.count();

        LocalDate today = LocalDate.now();
        AttendanceSnapshot attendance = loadAttendanceSnapshot(today, totalEmployees);

        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        long onboardingThisMonth = employeeRepository.countByOnboardingDateBetween(firstDayOfMonth, lastDayOfMonth);

        long pendingApprovals = departmentProjectApplicationRepository.countByApplicationStatus(
                DepartmentApplicationStatus.SUBMITTED_TO_HR);

        int internalPercent = totalEmployees > 0 ? (int) ((internalEmployees * 100) / totalEmployees) : 0;
        int externalPercent = totalEmployees > 0 ? (int) ((externalEmployees * 100) / totalEmployees) : 0;

        List<DepartmentOnboardingView> departmentOnboarding = employeeRepository
                .summarizeEmployeeCountsByDepartment()
                .stream()
                .filter(summary -> hasText(summary.getDepartment()))
                .sorted(Comparator.comparing(
                        EmployeeDepartmentCountProjection::getDepartment,
                        String.CASE_INSENSITIVE_ORDER))
                .map(summary -> {
                    int count = toInt(projectionCount(summary.getEmployeeCount()));
                    return new DepartmentOnboardingView(summary.getDepartment().trim(), count, count + 10);
                })
                .limit(5)
                .toList();

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
                attendance.presentEmployees(),
                attendance.absentEmployees(),
                attendance.presentPercent(),
                attendance.checkIns(),
                (int) pendingApprovals,
                totalWings,
                totalCells,
                internalPercent,
                externalPercent,
                departmentOnboarding,
                internalProjectList,
                externalProjectList
        );
    }

    @Override
    @Transactional(readOnly = true)
    public HRTodayAttendanceView getTodayAttendance() {
        LocalDate today = LocalDate.now();
        AttendanceSnapshot attendance = loadAttendanceSnapshot(today, employeeRepository.count());
        return new HRTodayAttendanceView(
                today,
                attendance.totalEmployees(),
                attendance.presentEmployees(),
                attendance.absentEmployees(),
                attendance.presentPercent(),
                attendance.checkIns(),
                buildCellAttendance(today));
    }

    @Override
    @Transactional(readOnly = true)
    public HRWingReportsView getWingReports() {
        List<HRWingDirectoryItemView> wings = buildWingDirectory();
        return new HRWingReportsView(
                wings.size(),
                wings.stream().mapToInt(HRWingDirectoryItemView::cellCount).sum(),
                wings.stream().mapToInt(HRWingDirectoryItemView::projectCount).sum(),
                wings.stream().mapToInt(HRWingDirectoryItemView::employeeCount).sum(),
                wings);
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
                    List<EmployeeCellMappingEntity> employeeMappings = employeeCellMappingRepository
                            .findActiveEmployeeMappingsByWingId(
                                    wingId,
                                    ACTIVE_FLAG,
                                    ACTIVE_EMPLOYEE_STATUS);
                    Map<Long, Integer> employeeCountsByCellId = buildEmployeeCountsByCellId(employeeMappings);
                    Map<Long, List<HREmployeeHierarchyView>> employeesByCellId =
                            buildEmployeeHierarchyByCellId(employeeMappings);

                    return toWingReportView(
                            wing,
                            cells,
                            projectCountsByCellId,
                             employeeCountsByCellId,
                             maxCount(cells, projectCountsByCellId),
                             maxCount(cells, employeeCountsByCellId),
                             employeesByCellId);
                });
    }

    private List<HRWingDirectoryItemView> buildWingDirectory() {
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
        return wings.stream()
                .map(wing -> toWingDirectoryItem(
                        wing,
                        cellsByWingId.getOrDefault(wing.getWingId(), List.of()),
                        projectCountsByCellId,
                        employeeCountsByCellId))
                .toList();
    }

    private HRWingDirectoryItemView toWingDirectoryItem(
            WingMaster wing,
            List<CellMaster> cells,
            Map<Long, Integer> projectCountsByCellId,
            Map<Long, Integer> employeeCountsByCellId) {
        int projectCount = sumCounts(cells, projectCountsByCellId);
        int employeeCount = sumCounts(cells, employeeCountsByCellId);
        return new HRWingDirectoryItemView(
                wing.getWingId(),
                defaultText(wing.getWingName(), "Unnamed Wing"),
                cells.size(),
                projectCount,
                employeeCount);
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
        return toEmployeeCountMap(employeeCellMappingRepository.summarizeActiveEmployeesByCell(
                ACTIVE_FLAG,
                ACTIVE_EMPLOYEE_STATUS));
    }

    private Map<Long, Integer> toEmployeeCountMap(List<EmployeeCellCountProjection> summaries) {
        return summaries.stream()
                .filter(summary -> summary.getCellId() != null)
                .collect(Collectors.toMap(
                        EmployeeCellCountProjection::getCellId,
                        summary -> toInt(summary.getEmployeeCount()),
                        Integer::sum));
    }

    private HRWingReportView toWingReportView(
            WingMaster wing,
            List<CellMaster> cells,
            Map<Long, Integer> projectCountsByCellId,
                         Map<Long, Integer> employeeCountsByCellId,
                         int maxProjectCount,
                         int maxEmployeeCount,
                         Map<Long, List<HREmployeeHierarchyView>> employeesByCellId) {
        List<HRCellReportView> cellReports = cells.stream()
                .sorted(Comparator.comparing(
                        cell -> defaultText(cell.getCellName(), ""),
                        String.CASE_INSENSITIVE_ORDER))
                .map(cell -> toCellReportView(
                        cell,
                        projectCountsByCellId.getOrDefault(cell.getCellId(), 0),
                         employeeCountsByCellId.getOrDefault(cell.getCellId(), 0),
                         maxProjectCount,
                         maxEmployeeCount,
                         employeesByCellId.getOrDefault(cell.getCellId(), List.of())))
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
                         int maxEmployeeCount,
                         List<HREmployeeHierarchyView> employees) {
        return new HRCellReportView(
                cell.getCellId(),
                defaultText(cell.getCellName(), "Unnamed Cell"),
                projectCount,
                 employeeCount,
                 percent(projectCount, maxProjectCount),
                 percent(employeeCount, maxEmployeeCount),
                 employees);
    }

    private Map<Long, Integer> buildEmployeeCountsByCellId(
            List<EmployeeCellMappingEntity> employeeMappings) {
        return employeeMappings.stream()
                .filter(this::hasEmployeeAndCell)
                .collect(Collectors.groupingBy(
                        mapping -> mapping.getCell().getCellId(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(
                                        mapping -> mapping.getEmployee().getEmployeeId(),
                                        Collectors.toSet()),
                                Set::size)));
    }

    private Map<Long, List<HREmployeeHierarchyView>> buildEmployeeHierarchyByCellId(
            List<EmployeeCellMappingEntity> employeeMappings) {
        Map<Long, Map<Long, EmployeeEntity>> employeesByCellId = employeeMappings.stream()
                .filter(this::hasEmployeeAndCell)
                .collect(Collectors.groupingBy(
                        mapping -> mapping.getCell().getCellId(),
                        LinkedHashMap::new,
                        Collectors.toMap(
                                mapping -> mapping.getEmployee().getEmployeeId(),
                                EmployeeCellMappingEntity::getEmployee,
                                (existing, ignored) -> existing,
                                LinkedHashMap::new)));
        if (employeesByCellId.isEmpty()) {
            return Map.of();
        }

        List<Long> employeeIds = employeesByCellId.values().stream()
                .flatMap(employees -> employees.keySet().stream())
                .distinct()
                .toList();
        Map<Long, EmployeeReportingMappingEntity> reportingByEmployeeId =
                employeeReportingMappingRepository.findByEmployeeIdIn(employeeIds).stream()
                        .filter(mapping -> mapping != null && mapping.getEmployeeId() != null)
                        .collect(Collectors.toMap(
                                EmployeeReportingMappingEntity::getEmployeeId,
                                Function.identity(),
                                this::latestReportingMapping));

        Map<Long, CellReportingAuthorityMappingEntity> authorityByCellId =
                cellReportingAuthorityMappingRepository.findByCellCellIdIn(employeesByCellId.keySet()).stream()
                        .filter(mapping -> mapping != null
                                && mapping.getCell() != null
                                && mapping.getCell().getCellId() != null)
                        .collect(Collectors.toMap(
                                mapping -> mapping.getCell().getCellId(),
                                Function.identity(),
                                this::latestCellAuthorityMapping));

        Map<Long, Long> employeeIdByUserId = employeesByCellId.values().stream()
                .flatMap(employees -> employees.values().stream())
                .filter(employee -> employee.getUser() != null && employee.getUser().getId() != null)
                .collect(Collectors.toMap(
                        employee -> employee.getUser().getId(),
                        EmployeeEntity::getEmployeeId,
                        (existing, ignored) -> existing));

        Map<Long, List<HREmployeeHierarchyView>> hierarchyByCellId = new LinkedHashMap<>();
        employeesByCellId.forEach((cellId, employeesById) -> hierarchyByCellId.put(
                cellId,
                buildCellEmployeeHierarchy(
                        employeesById,
                        reportingByEmployeeId,
                        authorityByCellId.get(cellId),
                        employeeIdByUserId)));
        return hierarchyByCellId;
    }

    private List<HREmployeeHierarchyView> buildCellEmployeeHierarchy(
            Map<Long, EmployeeEntity> employeesById,
            Map<Long, EmployeeReportingMappingEntity> reportingByEmployeeId,
            CellReportingAuthorityMappingEntity cellAuthority,
            Map<Long, Long> employeeIdByUserId) {
        Map<Long, Long> candidateParentByEmployeeId = new LinkedHashMap<>();
        Map<Long, String> sourceByEmployeeId = new LinkedHashMap<>();

        employeesById.forEach((employeeId, employee) -> {
            EmployeeReportingMappingEntity directMapping = reportingByEmployeeId.get(employeeId);
            Long candidateParentId;
            String reportingSource;
            if (directMapping != null) {
                candidateParentId = directMapping.getManagerEmployeeId() != null
                        ? directMapping.getManagerEmployeeId()
                        : employeeIdByUserId.get(directMapping.getHodUserId());
                reportingSource = REPORTING_SOURCE_DIRECT;
            } else if (cellAuthority != null) {
                candidateParentId = employeeIdByUserId.get(cellAuthority.getAuthorityUserId());
                reportingSource = REPORTING_SOURCE_CELL;
            } else {
                candidateParentId = null;
                reportingSource = REPORTING_SOURCE_NONE;
            }

            if (candidateParentId == null
                    || candidateParentId.equals(employeeId)
                    || !employeesById.containsKey(candidateParentId)) {
                candidateParentId = null;
            }
            candidateParentByEmployeeId.put(employeeId, candidateParentId);
            sourceByEmployeeId.put(employeeId, reportingSource);
        });

        Map<Long, Long> parentByEmployeeId = new LinkedHashMap<>();
        candidateParentByEmployeeId.forEach((employeeId, parentId) -> parentByEmployeeId.put(
                employeeId,
                createsReportingCycle(employeeId, parentId, candidateParentByEmployeeId) ? null : parentId));

        Map<Long, List<Long>> childrenByEmployeeId = new LinkedHashMap<>();
        employeesById.keySet().forEach(employeeId -> childrenByEmployeeId.put(employeeId, new ArrayList<>()));
        parentByEmployeeId.forEach((employeeId, parentId) -> {
            if (parentId != null) {
                childrenByEmployeeId.get(parentId).add(employeeId);
            }
        });

        Comparator<Long> employeeIdComparator = Comparator
                .<Long, String>comparing(
                        employeeId -> employeeName(employeesById.get(employeeId)),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(Long::longValue);
        childrenByEmployeeId.values().forEach(children -> children.sort(employeeIdComparator));

        List<Long> roots = parentByEmployeeId.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .sorted(Comparator
                        .<Long>comparingInt(employeeId -> childrenByEmployeeId.get(employeeId).size())
                        .reversed()
                        .thenComparing(employeeIdComparator))
                .toList();

        List<HREmployeeHierarchyView> hierarchy = new ArrayList<>(employeesById.size());
        Set<Long> visited = new HashSet<>();
        appendHierarchy(roots, 0, employeesById, parentByEmployeeId, childrenByEmployeeId,
                sourceByEmployeeId, visited, hierarchy);

        List<Long> remaining = employeesById.keySet().stream()
                .filter(employeeId -> !visited.contains(employeeId))
                .sorted(employeeIdComparator)
                .toList();
        appendHierarchy(remaining, 0, employeesById, parentByEmployeeId, childrenByEmployeeId,
                sourceByEmployeeId, visited, hierarchy);
        return List.copyOf(hierarchy);
    }

    private void appendHierarchy(
            List<Long> employeeIds,
            int initialDepth,
            Map<Long, EmployeeEntity> employeesById,
            Map<Long, Long> parentByEmployeeId,
            Map<Long, List<Long>> childrenByEmployeeId,
            Map<Long, String> sourceByEmployeeId,
            Set<Long> visited,
            List<HREmployeeHierarchyView> hierarchy) {
        Deque<HierarchyCursor> pending = new ArrayDeque<>();
        for (int index = employeeIds.size() - 1; index >= 0; index--) {
            pending.push(new HierarchyCursor(employeeIds.get(index), initialDepth));
        }

        while (!pending.isEmpty()) {
            HierarchyCursor cursor = pending.pop();
            if (!visited.add(cursor.employeeId())) {
                continue;
            }
            EmployeeEntity employee = employeesById.get(cursor.employeeId());
            Long parentId = parentByEmployeeId.get(cursor.employeeId());
            EmployeeEntity parent = parentId == null ? null : employeesById.get(parentId);
            List<Long> children = childrenByEmployeeId.getOrDefault(cursor.employeeId(), List.of());
            String name = employeeName(employee);
            hierarchy.add(new HREmployeeHierarchyView(
                    employee.getEmployeeId(),
                    defaultText(employee.getEmployeeCode(), "-"),
                    name,
                    initials(name),
                    defaultText(employee.getPhotoPath(), ""),
                    employee.getDesignation() == null
                            ? "Designation not assigned"
                            : defaultText(employee.getDesignation().getDesignationName(), "Designation not assigned"),
                    cursor.depth(),
                    children.size(),
                    parent == null ? "" : employeeName(parent),
                    sourceByEmployeeId.getOrDefault(cursor.employeeId(), REPORTING_SOURCE_NONE)));

            for (int index = children.size() - 1; index >= 0; index--) {
                pending.push(new HierarchyCursor(children.get(index), cursor.depth() + 1));
            }
        }
    }

    private boolean createsReportingCycle(
            Long employeeId,
            Long parentId,
            Map<Long, Long> candidateParentByEmployeeId) {
        Set<Long> visited = new HashSet<>();
        visited.add(employeeId);
        Long currentId = parentId;
        while (currentId != null) {
            if (!visited.add(currentId)) {
                return true;
            }
            currentId = candidateParentByEmployeeId.get(currentId);
        }
        return false;
    }

    private EmployeeReportingMappingEntity latestReportingMapping(
            EmployeeReportingMappingEntity first,
            EmployeeReportingMappingEntity second) {
        return nullableId(first.getMappingId()) >= nullableId(second.getMappingId()) ? first : second;
    }

    private CellReportingAuthorityMappingEntity latestCellAuthorityMapping(
            CellReportingAuthorityMappingEntity first,
            CellReportingAuthorityMappingEntity second) {
        return nullableId(first.getMappingId()) >= nullableId(second.getMappingId()) ? first : second;
    }

    private boolean hasEmployeeAndCell(EmployeeCellMappingEntity mapping) {
        return mapping != null
                && mapping.getEmployee() != null
                && mapping.getEmployee().getEmployeeId() != null
                && mapping.getCell() != null
                && mapping.getCell().getCellId() != null;
    }

    private String employeeName(EmployeeEntity employee) {
        return employee == null ? "Employee" : defaultText(employee.getFullName(), "Employee");
    }

    private String initials(String value) {
        String[] words = defaultText(value, "E").split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(words[0].length(), 2)).toUpperCase();
        }
        return (String.valueOf(words[0].charAt(0)) + words[words.length - 1].charAt(0)).toUpperCase();
    }

    private long nullableId(Long value) {
        return value == null ? Long.MIN_VALUE : value;
    }

    private record HierarchyCursor(Long employeeId, int depth) {
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

    private long countExternalPresentEmployees(LocalDate today) {
        return attendanceRegisterRepo.countExternalPresentByMonthYearDay(
                today.getMonthValue(),
                today.getYear(),
                today.getDayOfMonth());
    }

    private int sumCounts(List<CellMaster> cells, Map<Long, Integer> countsByCellId) {
        return cells.stream()
                .filter(cell -> cell.getCellId() != null)
                .mapToInt(cell -> countsByCellId.getOrDefault(cell.getCellId(), 0))
                .sum();
    }

    private AttendanceSnapshot loadAttendanceSnapshot(LocalDate attendanceDate, long totalEmployeeCount) {
        AttendanceCheckInSummaryProjection projection = dailyAttendanceInternalRepository
                .summarizeAttendanceByDate(
                        attendanceDate,
                        EARLY_CHECK_IN_CUTOFF,
                        LATE_CHECK_IN_CUTOFF);
        long rawPresentCount = projectionCount(projection == null ? null : projection.getPresentCount())
                + countExternalPresentEmployees(attendanceDate);
        int totalEmployees = toInt(Math.max(totalEmployeeCount, 0));
        int presentEmployees = Math.min(totalEmployees, toInt(rawPresentCount));
        HRAttendanceSummaryView checkIns = new HRAttendanceSummaryView(
                toInt(projectionCount(projection == null ? null : projection.getCheckedInCount())),
                toInt(projectionCount(projection == null ? null : projection.getEarlyCount())),
                toInt(projectionCount(projection == null ? null : projection.getStandardCount())),
                toInt(projectionCount(projection == null ? null : projection.getLateCount())));
        return new AttendanceSnapshot(
                totalEmployees,
                presentEmployees,
                totalEmployees - presentEmployees,
                percent(presentEmployees, totalEmployees),
                checkIns);
    }

    private List<HRCellAttendanceView> buildCellAttendance(LocalDate attendanceDate) {
        return dailyAttendanceInternalRepository.summarizeAttendanceByCell(
                        attendanceDate,
                        attendanceDate.getMonthValue(),
                        attendanceDate.getYear(),
                        attendanceDate.getDayOfMonth(),
                        ACTIVE_FLAG,
                        ACTIVE_EMPLOYEE_STATUS)
                .stream()
                .map(this::toCellAttendanceView)
                .toList();
    }

    private HRCellAttendanceView toCellAttendanceView(AttendanceCellSummaryProjection summary) {
        int totalEmployees = toInt(projectionCount(summary.getTotalEmployees()));
        int presentEmployees = Math.min(
                totalEmployees,
                toInt(projectionCount(summary.getPresentEmployees())));
        return new HRCellAttendanceView(
                summary.getCellId(),
                defaultText(summary.getCellName(), "Unnamed Cell"),
                defaultText(summary.getWingName(), "Unnamed Wing"),
                totalEmployees,
                presentEmployees,
                totalEmployees - presentEmployees,
                percent(presentEmployees, totalEmployees));
    }

    private long projectionCount(Long value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private record AttendanceSnapshot(
            int totalEmployees,
            int presentEmployees,
            int absentEmployees,
            int presentPercent,
            HRAttendanceSummaryView checkIns) {
    }
}
