package com.maharecruitment.gov.in.web.service.dashboard;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeProjectMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.CellReportingAuthorityMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeProjectMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HodDashboardService {
    private final ReportingManagerService reportingManagerService;
    private final EmployeeRepository employeeRepository;
    private final EmployeeCellMappingRepository employeeCellMappingRepository;
    private final EmployeeProjectMappingRepository employeeProjectMappingRepository;
    private final CellReportingAuthorityMappingRepository cellReportingAuthorityMappingRepository;
    private final CellMasterRepository cellMasterRepository;
    private final ProjectMstRepository projectMstRepository;
    private final DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

    @Transactional(readOnly = true)
    public HodDashboardView getDashboard(Long hodUserId) {
        if (hodUserId == null) {
            return HodDashboardView.empty();
        }
        List<Long> employeeIds = reportingManagerService.getEffectiveEmployeeIdsForAuthority(hodUserId);
        List<EmployeeCellMappingEntity> employeeCellMappings = employeeIds.isEmpty()
                ? List.of()
                : employeeCellMappingRepository.findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAsc(employeeIds);
        List<EmployeeProjectMappingEntity> employeeProjectMappings = employeeIds.isEmpty()
                ? List.of()
                : employeeProjectMappingRepository.findByEmployeeEmployeeIdIn(employeeIds);

        Map<Long, CellMaster> cellsById = new LinkedHashMap<>();
        employeeCellMappings.forEach(mapping -> addCell(cellsById, mapping.getCell()));
        List<Long> directlyAssignedCellIds = cellReportingAuthorityMappingRepository
                .findCellIdsByAuthorityUserId(hodUserId).stream()
                .filter(cellId -> !cellsById.containsKey(cellId))
                .toList();
        if (!directlyAssignedCellIds.isEmpty()) {
            cellMasterRepository.findByCellIdIn(directlyAssignedCellIds)
                    .forEach(cell -> addCell(cellsById, cell));
        }

        Map<Long, EmployeeCellMappingEntity> cellMappingsByEmployeeId = cellMappingsByEmployeeId(employeeCellMappings);
        Map<Long, EmployeeProjectMappingEntity> projectMappingsByEmployeeId = projectMappingsByEmployeeId(employeeProjectMappings);
        Map<Long, Integer> employeeCountsByCellId = countEmployeesByCell(employeeCellMappings);
        Map<Long, ProjectMst> projectsById = relevantProjects(cellsById.keySet(), employeeProjectMappings);
        Map<Long, Integer> employeeCountsByProjectId = countEmployeesByProject(employeeProjectMappings);
        Map<Long, Integer> projectCountsByCellId = countProjectsByCell(projectsById.values());

        Map<Long, EmployeeView> employeeViewsById = new LinkedHashMap<>();
        if (!employeeIds.isEmpty()) {
            employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(employeeIds).forEach(employee ->
                    employeeViewsById.put(employee.getEmployeeId(), toEmployeeView(
                            employee,
                            cellMappingsByEmployeeId.get(employee.getEmployeeId()),
                            projectMappingsByEmployeeId.get(employee.getEmployeeId()))));
        }
        List<EmployeeView> employees = List.copyOf(employeeViewsById.values());
        Set<Long> presentEmployeeIds = presentEmployeeIds(employeeViewsById.keySet(), LocalDate.now());
        List<EmployeeView> presentEmployees = employeeViewsById.entrySet().stream()
                .filter(entry -> presentEmployeeIds.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        List<EmployeeView> absentEmployees = employeeViewsById.entrySet().stream()
                .filter(entry -> !presentEmployeeIds.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        List<CellView> cells = cellsById.values().stream()
                .map(cell -> new CellView(
                        cell.getCellId(),
                        text(cell.getCellName(), "Unnamed Cell"),
                        cell.getWing() == null ? "-" : text(cell.getWing().getWingName(), "-"),
                        employeeCountsByCellId.getOrDefault(cell.getCellId(), 0),
                        projectCountsByCellId.getOrDefault(cell.getCellId(), 0)))
                .sorted(Comparator.comparing(CellView::cellName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<ProjectView> projects = projectsById.values().stream()
                .map(project -> new ProjectView(
                        project.getProjectId(),
                        text(project.getProjectName(), "Unnamed Project"),
                        text(project.getProjectCode(), "-"),
                        project.getCell() == null ? "-" : text(project.getCell().getCellName(), "-"),
                        employeeCountsByProjectId.getOrDefault(project.getProjectId(), 0)))
                .sorted(Comparator.comparing(ProjectView::projectName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new HodDashboardView(
                employees.size(),
                cells.size(),
                projects.size(),
                presentEmployees.size(),
                absentEmployees.size(),
                employees,
                cells,
                projects,
                presentEmployees,
                absentEmployees);
    }

    /** Kept for callers that need only the employee directory. */
    @Transactional(readOnly = true)
    public List<EmployeeView> getEmployees(Long hodUserId) {
        return getDashboard(hodUserId).employees();
    }

    private Map<Long, EmployeeCellMappingEntity> cellMappingsByEmployeeId(List<EmployeeCellMappingEntity> mappings) {
        Map<Long, EmployeeCellMappingEntity> result = new HashMap<>();
        mappings.forEach(mapping -> {
            if (mapping.getEmployee() != null && mapping.getEmployee().getEmployeeId() != null) {
                result.put(mapping.getEmployee().getEmployeeId(), mapping);
            }
        });
        return result;
    }

    private Map<Long, EmployeeProjectMappingEntity> projectMappingsByEmployeeId(
            List<EmployeeProjectMappingEntity> mappings) {
        Map<Long, EmployeeProjectMappingEntity> result = new HashMap<>();
        mappings.forEach(mapping -> {
            if (mapping.getEmployee() != null && mapping.getEmployee().getEmployeeId() != null) {
                result.put(mapping.getEmployee().getEmployeeId(), mapping);
            }
        });
        return result;
    }

    private Map<Long, Integer> countEmployeesByCell(List<EmployeeCellMappingEntity> mappings) {
        Map<Long, Integer> counts = new HashMap<>();
        mappings.forEach(mapping -> {
            if (mapping.getCell() != null && mapping.getCell().getCellId() != null) {
                counts.merge(mapping.getCell().getCellId(), 1, Integer::sum);
            }
        });
        return counts;
    }

    private Map<Long, ProjectMst> relevantProjects(
            Set<Long> cellIds,
            List<EmployeeProjectMappingEntity> employeeProjectMappings) {
        Map<Long, ProjectMst> projects = new LinkedHashMap<>();
        employeeProjectMappings.forEach(mapping -> addProject(projects, mapping.getProject()));
        if (!cellIds.isEmpty()) {
            projectMstRepository.findByCell_CellIdInAndActiveFlagIgnoreCaseOrderByProjectNameAsc(cellIds, "Y").stream()
                    .forEach(project -> addProject(projects, project));
        }
        return projects;
    }

    private Map<Long, Integer> countEmployeesByProject(List<EmployeeProjectMappingEntity> mappings) {
        Map<Long, Integer> counts = new HashMap<>();
        mappings.forEach(mapping -> {
            if (mapping.getProject() != null && mapping.getProject().getProjectId() != null) {
                counts.merge(mapping.getProject().getProjectId(), 1, Integer::sum);
            }
        });
        return counts;
    }

    private Set<Long> presentEmployeeIds(Collection<Long> employeeIds, LocalDate attendanceDate) {
        if (employeeIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> presentEmployeeIds = new LinkedHashSet<>();
        for (DailyAttendanceInternalEntity attendance : dailyAttendanceInternalRepository
                .findByEmployeeIdInAndAttendanceDateBetween(employeeIds, attendanceDate, attendanceDate)) {
            if (attendance.getEmployeeId() != null
                    && "PRESENT".equalsIgnoreCase(text(attendance.getStatus(), ""))) {
                presentEmployeeIds.add(attendance.getEmployeeId());
            }
        }
        return presentEmployeeIds;
    }

    private Map<Long, Integer> countProjectsByCell(Collection<ProjectMst> projects) {
        Map<Long, Integer> counts = new HashMap<>();
        projects.forEach(project -> {
            if (project.getCell() != null && project.getCell().getCellId() != null) {
                counts.merge(project.getCell().getCellId(), 1, Integer::sum);
            }
        });
        return counts;
    }

    private void addCell(Map<Long, CellMaster> cellsById, CellMaster cell) {
        if (cell != null && cell.getCellId() != null) {
            cellsById.putIfAbsent(cell.getCellId(), cell);
        }
    }

    private void addProject(Map<Long, ProjectMst> projectsById, ProjectMst project) {
        if (project != null && project.getProjectId() != null) {
            projectsById.putIfAbsent(project.getProjectId(), project);
        }
    }

    private EmployeeView toEmployeeView(
            com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity employee,
            EmployeeCellMappingEntity cellMapping,
            EmployeeProjectMappingEntity projectMapping) {
        return new EmployeeView(
                text(employee.getEmployeeCode(), "-"),
                text(employee.getFullName(), "Employee"),
                employee.getDesignation() == null ? "-" : text(employee.getDesignation().getDesignationName(), "-"),
                text(employee.getEmail(), "-"),
                text(employee.getRecruitmentType(), "-"),
                text(employee.getStatus(), "-"),
                cellMapping == null || cellMapping.getCell() == null ? "-"
                        : text(cellMapping.getCell().getCellName(), "-"),
                projectMapping == null || projectMapping.getProject() == null ? "-"
                        : text(projectMapping.getProject().getProjectName(), "-"));
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record HodDashboardView(
            int totalEmployees,
            int totalCells,
            int totalProjects,
            int presentEmployeesCount,
            int absentEmployeesCount,
            List<EmployeeView> employees,
            List<CellView> cells,
            List<ProjectView> projects,
            List<EmployeeView> presentEmployees,
            List<EmployeeView> absentEmployees) {
        private static HodDashboardView empty() {
            return new HodDashboardView(0, 0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    public record EmployeeView(
            String employeeCode,
            String fullName,
            String designation,
            String email,
            String recruitmentType,
            String status,
            String cellName,
            String projectName) {
    }

    public record CellView(Long cellId, String cellName, String wingName, int employeeCount, int projectCount) {
    }

    public record ProjectView(Long projectId, String projectName, String projectCode, String cellName,
            int employeeCount) {
    }
}
