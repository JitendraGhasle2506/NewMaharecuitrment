package com.maharecruitment.gov.in.master.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.ProjectRequest;
import com.maharecruitment.gov.in.master.dto.ProjectResponse;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.entity.ProjectType;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.ProjectMapper;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.master.service.ProjectMstService;

@Service
@Transactional(readOnly = true)
public class ProjectMstServiceImpl implements ProjectMstService {

    private static final String ACTIVE = "Y";
    private static final String INACTIVE = "N";

    private final ProjectMstRepository projectRepository;
    private final CellMasterRepository cellMasterRepository;
    private final DepartmentMstRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final ProjectMapper projectMapper;

    public ProjectMstServiceImpl(
            ProjectMstRepository projectRepository,
            CellMasterRepository cellMasterRepository,
            DepartmentMstRepository departmentRepository,
            SubDepartmentRepository subDepartmentRepository,
            ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.cellMasterRepository = cellMasterRepository;
        this.departmentRepository = departmentRepository;
        this.subDepartmentRepository = subDepartmentRepository;
        this.projectMapper = projectMapper;
    }

    @Override
    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        String projectName = normalizeName(request.getProjectName());
        DepartmentSelection departmentSelection = resolveDepartmentSelection(
                request.getDepartmentId(),
                request.getSubDepartmentId());
        ensureUniqueProject(
                projectName,
                departmentSelection.department().getDepartmentId(),
                departmentSelection.subDepartmentId(),
                null);
        String projectCode = normalizeCode(request.getProjectCode());
        ensureUniqueProjectCode(projectCode, null);

        ProjectMst entity = new ProjectMst();
        mapRequestToEntity(request, entity, projectName, projectCode, departmentSelection);

        return projectMapper.toResponse(projectRepository.save(entity));
    }

    @Override
    @Transactional
    public ProjectResponse update(Long projectId, ProjectRequest request) {
        ProjectMst entity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for id: " + projectId));

        String projectName = normalizeName(request.getProjectName());
        DepartmentSelection departmentSelection = resolveDepartmentSelection(
                request.getDepartmentId(),
                request.getSubDepartmentId());
        ensureUniqueProject(
                projectName,
                departmentSelection.department().getDepartmentId(),
                departmentSelection.subDepartmentId(),
                projectId);
        String projectCode = normalizeCode(request.getProjectCode());
        ensureUniqueProjectCode(projectCode, projectId);
        mapRequestToEntity(request, entity, projectName, projectCode, departmentSelection);

        return projectMapper.toResponse(projectRepository.save(entity));
    }

    @Override
    public ProjectResponse getById(Long projectId) {
        ProjectMst entity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for id: " + projectId));
        return projectMapper.toResponse(entity);
    }

    @Override
    public Page<ProjectResponse> getAll(boolean includeInactive, Pageable pageable) {
        Page<ProjectMst> projects = includeInactive
                ? projectRepository.findAll(pageable)
                : projectRepository.findByActiveFlagIgnoreCase(ACTIVE, pageable);
        return projects.map(projectMapper::toResponse);
    }

    @Override
    public Page<ProjectResponse> getAll(Long cellId, boolean includeInactive, Pageable pageable) {
        if (cellId == null) {
            return getAll(includeInactive, pageable);
        }
        Page<ProjectMst> projects = includeInactive
                ? projectRepository.findByCell_CellId(cellId, pageable)
                : projectRepository.findByCell_CellIdAndActiveFlagIgnoreCase(cellId, ACTIVE, pageable);
        return projects.map(projectMapper::toResponse);
    }

    @Override
    @Transactional
    public void softDelete(Long projectId) {
        ProjectMst entity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for id: " + projectId));
        entity.setActiveFlag(INACTIVE);
        projectRepository.save(entity);
    }

    @Override
    @Transactional
    public void restore(Long projectId) {
        ProjectMst entity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for id: " + projectId));
        entity.setActiveFlag(ACTIVE);
        projectRepository.save(entity);
    }

    @Override
    @Transactional
    public ProjectResponse upsertFromDepartmentApplication(
            String projectName,
            ProjectType projectType,
            Long departmentId,
            Long subDepartmentId,
            Long applicationId) {
        if (applicationId == null) {
            throw new BusinessValidationException("Application id is required to sync project master.");
        }
        if (departmentId == null) {
            throw new BusinessValidationException("Department id is required to sync project master.");
        }
        if (projectType == null) {
            throw new BusinessValidationException("Project type is required to sync project master.");
        }

        String normalizedProjectName = normalizeName(projectName);
        if (normalizedProjectName == null || normalizedProjectName.isBlank()) {
            throw new BusinessValidationException("Project name is required to sync project master.");
        }

        DepartmentSelection departmentSelection = resolveDepartmentSelection(departmentId, subDepartmentId);

        ProjectMst entity = projectRepository.findFirstByApplicationId(applicationId)
                .orElseGet(() -> projectRepository
                        .findFirstByProjectNameIgnoreCaseAndDepartmentIdAndSubDepartmentId(
                                normalizedProjectName,
                                departmentId,
                                subDepartmentId)
                        .orElseGet(ProjectMst::new));

        entity.setProjectName(normalizedProjectName);
        if (entity.getProjectCode() == null || entity.getProjectCode().isBlank()) {
            entity.setProjectCode(generateSyncedProjectCode(applicationId, normalizedProjectName));
        }
        entity.setProjectType(projectType);
        entity.setProjectScopeType(ProjectScopeType.EXTERNAL);
        applyDepartmentSelection(entity, departmentSelection);
        entity.setApplicationId(applicationId);
        entity.setActiveFlag(ACTIVE);

        return projectMapper.toResponse(projectRepository.save(entity));
    }

    private void mapRequestToEntity(
            ProjectRequest request,
            ProjectMst entity,
            String normalizedProjectName,
            String projectCode,
            DepartmentSelection departmentSelection) {
        entity.setProjectName(normalizedProjectName);
        entity.setProjectCode(projectCode);
        entity.setProjectDesc(normalizeDescription(request.getProjectDesc()));
        entity.setProjectType(request.getProjectType());
        entity.setProjectScopeType(resolveProjectScopeType(request.getProjectScopeType(), entity.getApplicationId()));
        applyDepartmentSelection(entity, departmentSelection);
        entity.setCell(resolveActiveCell(request.getCellId()));
    }

    private DepartmentSelection resolveDepartmentSelection(Long departmentId, Long subDepartmentId) {
        if (departmentId == null) {
            throw new BusinessValidationException("Department is required.");
        }

        DepartmentMst department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
        if (subDepartmentId == null) {
            return new DepartmentSelection(department, null);
        }

        SubDepartment subDepartment = subDepartmentRepository
                .findBySubDeptIdAndDepartmentDepartmentId(subDepartmentId, departmentId)
                .orElseThrow(() -> new BusinessValidationException(
                        "Selected sub-department does not belong to the selected department."));
        return new DepartmentSelection(department, subDepartment);
    }

    private void applyDepartmentSelection(ProjectMst entity, DepartmentSelection selection) {
        entity.setDepartmentId(selection.department().getDepartmentId());
        entity.setDepartment(selection.department());
        entity.setSubDepartmentId(selection.subDepartmentId());
        entity.setSubDepartment(selection.subDepartment());
    }

    private CellMaster resolveActiveCell(Long cellId) {
        if (cellId == null) {
            throw new BusinessValidationException("Cell is required.");
        }
        CellMaster cell = cellMasterRepository.findByCellId(cellId)
                .orElseThrow(() -> new ResourceNotFoundException("Cell not found with id: " + cellId));
        if (!"Y".equalsIgnoreCase(cell.getActiveFlag())) {
            throw new BusinessValidationException("Selected cell is inactive.");
        }
        if (cell.getWing() == null || !"Y".equalsIgnoreCase(cell.getWing().getActiveFlag())) {
            throw new BusinessValidationException("Selected cell belongs to an inactive wing.");
        }
        return cell;
    }

    private ProjectScopeType resolveProjectScopeType(ProjectScopeType projectScopeType, Long applicationId) {
        if (projectScopeType == null) {
            throw new BusinessValidationException("Project scope is required.");
        }
        if (applicationId != null && projectScopeType != ProjectScopeType.EXTERNAL) {
            throw new BusinessValidationException(
                    "Projects linked to department applications must remain external.");
        }
        return projectScopeType;
    }

    private void ensureUniqueProject(
            String projectName,
            Long departmentId,
            Long subDepartmentId,
            Long excludeId) {
        if (projectRepository.existsByProjectNameAndDepartmentAndSubDepartmentExcludingId(
                projectName,
                departmentId,
                subDepartmentId,
                excludeId)) {
            throw new DuplicateResourceException("Project already exists with name: " + projectName);
        }
    }

    private void ensureUniqueProjectCode(String projectCode, Long excludeId) {
        if (projectRepository.existsByProjectCodeExcludingId(projectCode, excludeId)) {
            throw new DuplicateResourceException("Project already exists with code: " + projectCode);
        }
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String generateSyncedProjectCode(Long applicationId, String projectName) {
        String base = "APP-" + applicationId;
        if (!projectRepository.existsByProjectCodeExcludingId(base, null)) {
            return base;
        }

        String prefix = projectName == null
                ? "PRJ"
                : projectName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (prefix.length() > 12) {
            prefix = prefix.substring(0, 12);
        }
        if (prefix.isBlank()) {
            prefix = "PRJ";
        }
        return prefix + "-" + applicationId;
    }

    private record DepartmentSelection(DepartmentMst department, SubDepartment subDepartment) {

        private Long subDepartmentId() {
            return subDepartment != null ? subDepartment.getSubDeptId() : null;
        }
    }
}
