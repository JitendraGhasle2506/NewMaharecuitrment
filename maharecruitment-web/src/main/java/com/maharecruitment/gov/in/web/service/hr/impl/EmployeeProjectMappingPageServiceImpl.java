package com.maharecruitment.gov.in.web.service.hr.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeProjectMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeRecruitmentType;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeProjectMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.service.hr.EmployeeProjectMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectBulkMappingResult;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectMappingEditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectMappingEmployeeView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectOptionView;

@Service
@Transactional(readOnly = true)
public class EmployeeProjectMappingPageServiceImpl implements EmployeeProjectMappingPageService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String ACTIVE_FLAG = "Y";

    private final EmployeeRepository employeeRepository;
    private final ProjectMstRepository projectRepository;
    private final EmployeeProjectMappingRepository mappingRepository;

    public EmployeeProjectMappingPageServiceImpl(
            EmployeeRepository employeeRepository,
            ProjectMstRepository projectRepository,
            EmployeeProjectMappingRepository mappingRepository) {
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.mappingRepository = mappingRepository;
    }

    @Override
    public Page<EmployeeProjectMappingEmployeeView> searchUnmappedEmployees(
            String recruitmentType,
            String searchText,
            Long projectId,
            Pageable pageable) {
        ProjectMst project = resolveNullableActiveProject(projectId);
        ProjectScopeType projectScope = project == null ? null : project.getProjectScopeType();
        Page<EmployeeEntity> employeePage = employeeRepository.findActiveOnboardedWithoutProjectMapping(
                normalizeRecruitmentType(recruitmentType),
                projectScope == null ? null : projectScope.name(),
                project == null ? null : project.getDepartmentId(),
                project == null ? null : project.getSubDepartmentId(),
                buildSearchPattern(searchText),
                pageable);
        List<EmployeeProjectMappingEmployeeView> content = employeePage.getContent().stream()
                .map(employee -> toEmployeeView(employee, null))
                .toList();
        return new PageImpl<>(content, pageable, employeePage.getTotalElements());
    }

    @Override
    public Page<EmployeeProjectMappingEmployeeView> searchMappedEmployees(
            String recruitmentType,
            String searchText,
            Pageable pageable) {
        Page<EmployeeEntity> employeePage = employeeRepository.findActiveOnboardedWithProjectMapping(
                normalizeRecruitmentType(recruitmentType),
                buildSearchPattern(searchText),
                pageable);
        Map<Long, EmployeeProjectMappingEntity> mappings = mappingsByEmployeeId(
                employeePage.getContent().stream().map(EmployeeEntity::getEmployeeId).toList());
        List<EmployeeProjectMappingEmployeeView> content = employeePage.getContent().stream()
                .map(employee -> toEmployeeView(employee, mappings.get(employee.getEmployeeId())))
                .toList();
        return new PageImpl<>(content, pageable, employeePage.getTotalElements());
    }

    @Override
    public EmployeeProjectMappingEditView loadMapping(Long employeeId) {
        EmployeeEntity employee = loadEligibleEmployee(employeeId);
        EmployeeProjectMappingEntity mapping = mappingRepository.findByEmployeeEmployeeId(employeeId).orElse(null);
        EmployeeProjectOptionView selectedProject = mapping == null ? null : toProjectOption(mapping.getProject());
        ProjectScopeType requiredScope = requiredScope(employee);
        List<EmployeeProjectOptionView> availableProjects = new ArrayList<>(projectRepository
                .findByProjectScopeTypeAndActiveFlagIgnoreCaseOrderByProjectNameAsc(requiredScope, ACTIVE_FLAG)
                .stream()
                .filter(project -> matchesEmployeeDepartment(project, employee))
                .map(this::toProjectOption)
                .toList());
        if (selectedProject != null
                && availableProjects.stream().noneMatch(project -> project.projectId().equals(selectedProject.projectId()))) {
            availableProjects.add(selectedProject);
        }
        return new EmployeeProjectMappingEditView(
                toEmployeeView(employee, mapping),
                List.copyOf(availableProjects),
                selectedProject,
                requiredScope.name());
    }

    @Override
    public List<EmployeeProjectOptionView> availableActiveProjects(String recruitmentType) {
        ProjectScopeType projectScope = projectScopeForFilter(recruitmentType);
        List<ProjectMst> projects = projectScope == null
                ? projectRepository.findByActiveFlagIgnoreCaseOrderByProjectNameAsc(ACTIVE_FLAG)
                : projectRepository.findByProjectScopeTypeAndActiveFlagIgnoreCaseOrderByProjectNameAsc(
                        projectScope,
                        ACTIVE_FLAG);
        return projects.stream()
                .map(this::toProjectOption)
                .toList();
    }

    @Override
    @Transactional
    public boolean updateMapping(Long employeeId, Long projectId) {
        EmployeeEntity employee = loadEligibleEmployee(employeeId);
        ProjectMst project = resolveActiveProject(projectId);
        ProjectScopeType requiredScope = requiredScope(employee);
        if (project.getProjectScopeType() != requiredScope) {
            throw new RecruitmentNotificationException(scopeMismatchMessage(requiredScope));
        }
        validateProjectDepartmentMatch(project, employee);

        EmployeeProjectMappingEntity mapping = mappingRepository.findByEmployeeEmployeeId(employeeId).orElse(null);
        if (mapping != null && Objects.equals(mapping.getProject().getProjectId(), project.getProjectId())) {
            return false;
        }
        if (mapping == null) {
            mapping = new EmployeeProjectMappingEntity();
            mapping.setEmployee(employee);
        }
        mapping.setProject(project);
        mappingRepository.save(mapping);
        return true;
    }

    @Override
    @Transactional
    public EmployeeProjectBulkMappingResult updateMappings(Long projectId, List<Long> employeeIds) {
        ProjectMst project = resolveActiveProject(projectId);
        List<Long> uniqueEmployeeIds = normalizeEmployeeIds(employeeIds);
        if (uniqueEmployeeIds.isEmpty()) {
            throw new RecruitmentNotificationException("Select at least one employee.");
        }

        List<EmployeeEntity> employees = employeeRepository.findDetailedByEmployeeIdIn(uniqueEmployeeIds);
        if (employees.size() != uniqueEmployeeIds.size()) {
            throw new RecruitmentNotificationException("One or more selected employees are invalid.");
        }
        Map<Long, EmployeeEntity> employeesById = employees.stream()
                .collect(Collectors.toMap(EmployeeEntity::getEmployeeId, Function.identity()));
        List<EmployeeEntity> orderedEmployees = uniqueEmployeeIds.stream()
                .map(employeesById::get)
                .toList();

        orderedEmployees.forEach(employee -> {
            validateEligibleEmployee(employee);
            ProjectScopeType requiredScope = requiredScope(employee);
            if (project.getProjectScopeType() != requiredScope) {
                throw new RecruitmentNotificationException(scopeMismatchMessage(requiredScope));
            }
            validateProjectDepartmentMatch(project, employee);
        });

        Map<Long, EmployeeProjectMappingEntity> mappings = mappingsByEmployeeId(uniqueEmployeeIds);
        List<EmployeeProjectMappingEntity> mappingsToSave = new ArrayList<>();
        int unchangedCount = 0;
        for (EmployeeEntity employee : orderedEmployees) {
            EmployeeProjectMappingEntity mapping = mappings.get(employee.getEmployeeId());
            if (mapping != null && Objects.equals(mapping.getProject().getProjectId(), project.getProjectId())) {
                unchangedCount++;
                continue;
            }
            if (mapping == null) {
                mapping = new EmployeeProjectMappingEntity();
                mapping.setEmployee(employee);
            }
            mapping.setProject(project);
            mappingsToSave.add(mapping);
        }
        if (!mappingsToSave.isEmpty()) {
            mappingRepository.saveAll(mappingsToSave);
        }
        return new EmployeeProjectBulkMappingResult(
                uniqueEmployeeIds.size(),
                mappingsToSave.size(),
                unchangedCount);
    }

    private EmployeeEntity loadEligibleEmployee(Long employeeId) {
        if (employeeId == null || employeeId < 1) {
            throw new RecruitmentNotificationException("Valid employee id is required.");
        }
        EmployeeEntity employee = employeeRepository.findDetailedByEmployeeId(employeeId)
                .orElseThrow(() -> new RecruitmentNotificationException("Employee not found."));
        validateEligibleEmployee(employee);
        return employee;
    }

    private void validateEligibleEmployee(EmployeeEntity employee) {
        if (!ACTIVE_STATUS.equalsIgnoreCase(employee.getStatus())) {
            throw new RecruitmentNotificationException("Project can be mapped only to active employees.");
        }
        if (!StringUtils.hasText(employee.getEmployeeCode())
                || "PENDING".equalsIgnoreCase(employee.getEmployeeCode().trim())
                || employee.getEmployeeCode().trim().toUpperCase().startsWith("TMP-")) {
            throw new RecruitmentNotificationException("Employee onboarding is not complete.");
        }
    }

    private ProjectMst resolveActiveProject(Long projectId) {
        if (projectId == null || projectId < 1) {
            throw new RecruitmentNotificationException("Select a valid project.");
        }
        ProjectMst project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RecruitmentNotificationException("Selected project was not found."));
        if (!ACTIVE_FLAG.equalsIgnoreCase(project.getActiveFlag())) {
            throw new RecruitmentNotificationException("Inactive projects cannot be mapped to an employee.");
        }
        return project;
    }

    private ProjectMst resolveNullableActiveProject(Long projectId) {
        if (projectId == null || projectId < 1) {
            return null;
        }
        return resolveActiveProject(projectId);
    }

    private ProjectScopeType projectScopeForFilter(String recruitmentType) {
        String normalizedRecruitmentType = normalizeRecruitmentType(recruitmentType);
        if (normalizedRecruitmentType == null) {
            return null;
        }
        return EmployeeRecruitmentType.EXTERNAL.name().equalsIgnoreCase(normalizedRecruitmentType)
                ? ProjectScopeType.EXTERNAL
                : ProjectScopeType.INTERNAL;
    }

    private ProjectScopeType requiredScope(EmployeeEntity employee) {
        return EmployeeRecruitmentType.EXTERNAL.name().equalsIgnoreCase(employee.getRecruitmentType())
                ? ProjectScopeType.EXTERNAL
                : ProjectScopeType.INTERNAL;
    }

    private String scopeMismatchMessage(ProjectScopeType requiredScope) {
        return requiredScope == ProjectScopeType.EXTERNAL
                ? "External employees can only be mapped to external projects."
                : "Internal and MAHAIT employees can only be mapped to internal projects.";
    }

    private Map<Long, EmployeeProjectMappingEntity> mappingsByEmployeeId(Collection<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return mappingRepository.findByEmployeeEmployeeIdIn(employeeIds).stream()
                .collect(Collectors.toMap(
                        mapping -> mapping.getEmployee().getEmployeeId(),
                        Function.identity(),
                        (left, right) -> left));
    }

    private List<Long> normalizeEmployeeIds(List<Long> employeeIds) {
        if (employeeIds == null) {
            return List.of();
        }
        return employeeIds.stream()
                .filter(Objects::nonNull)
                .filter(employeeId -> employeeId > 0)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));
    }

    private EmployeeProjectMappingEmployeeView toEmployeeView(
            EmployeeEntity employee,
            EmployeeProjectMappingEntity mapping) {
        return new EmployeeProjectMappingEmployeeView(
                employee.getEmployeeId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getDesignation() == null ? "-" : defaultIfBlank(
                        employee.getDesignation().getDesignationName(), "-"),
                resolveDepartmentDisplay(employee),
                resolveEmployeeDepartmentId(employee),
                resolveEmployeeSubDepartmentId(employee),
                defaultIfBlank(employee.getRecruitmentType(), "-"),
                mapping == null ? null : toProjectOption(mapping.getProject()));
    }

    private EmployeeProjectOptionView toProjectOption(ProjectMst project) {
        return new EmployeeProjectOptionView(
                project.getProjectId(),
                defaultIfBlank(project.getProjectName(), "-"),
                project.getProjectCode(),
                project.getDepartmentId(),
                project.getSubDepartmentId(),
                project.getProjectScopeType() == null ? "-" : project.getProjectScopeType().name(),
                ACTIVE_FLAG.equalsIgnoreCase(project.getActiveFlag()));
    }

    private void validateProjectDepartmentMatch(ProjectMst project, EmployeeEntity employee) {
        if (matchesEmployeeDepartment(project, employee)) {
            return;
        }
        Long projectSubDepartmentId = project == null ? null : project.getSubDepartmentId();
        if (projectSubDepartmentId != null) {
            throw new RecruitmentNotificationException(
                    "Selected project belongs to a different subdepartment than the employee.");
        }
        throw new RecruitmentNotificationException(
                "Selected project belongs to a different department than the employee.");
    }

    private boolean matchesEmployeeDepartment(ProjectMst project, EmployeeEntity employee) {
        if (project == null || employee == null) {
            return false;
        }

        Long projectSubDepartmentId = project.getSubDepartmentId();
        if (projectSubDepartmentId != null) {
            return Objects.equals(projectSubDepartmentId, resolveEmployeeSubDepartmentId(employee));
        }

        Long projectDepartmentId = project.getDepartmentId();
        return projectDepartmentId != null
                && Objects.equals(projectDepartmentId, resolveEmployeeDepartmentId(employee));
    }

    private Long resolveEmployeeDepartmentId(EmployeeEntity employee) {
        if (employee == null) {
            return null;
        }
        if (employee.getDepartment() != null) {
            return employee.getDepartment().getDepartmentId();
        }
        if (employee.getSubDepartment() != null && employee.getSubDepartment().getDepartment() != null) {
            return employee.getSubDepartment().getDepartment().getDepartmentId();
        }
        return null;
    }

    private Long resolveEmployeeSubDepartmentId(EmployeeEntity employee) {
        if (employee == null || employee.getSubDepartment() == null) {
            return null;
        }
        return employee.getSubDepartment().getSubDeptId();
    }

    private String resolveDepartmentDisplay(EmployeeEntity employee) {
        String departmentName = employee.getDepartment() == null
                ? null
                : employee.getDepartment().getDepartmentName();
        String subDepartmentName = employee.getSubDepartment() == null
                ? null
                : employee.getSubDepartment().getSubDeptName();

        String normalizedDepartment = defaultIfBlank(departmentName, null);
        String normalizedSubDepartment = defaultIfBlank(subDepartmentName, null);
        if (normalizedDepartment != null && normalizedSubDepartment != null) {
            return normalizedDepartment + " / " + normalizedSubDepartment;
        }
        if (normalizedDepartment != null) {
            return normalizedDepartment;
        }
        if (normalizedSubDepartment != null) {
            return normalizedSubDepartment;
        }
        return "-";
    }

    private String normalizeRecruitmentType(String recruitmentType) {
        if (!StringUtils.hasText(recruitmentType) || "ALL".equalsIgnoreCase(recruitmentType)) {
            return null;
        }
        return EmployeeRecruitmentType.normalizeOrNull(recruitmentType);
    }

    private String buildSearchPattern(String searchText) {
        return StringUtils.hasText(searchText) ? "%" + searchText.trim().toUpperCase() + "%" : null;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
