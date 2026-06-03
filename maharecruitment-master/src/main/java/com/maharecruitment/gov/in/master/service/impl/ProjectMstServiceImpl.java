package com.maharecruitment.gov.in.master.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.ProjectRequest;
import com.maharecruitment.gov.in.master.dto.ProjectResponse;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.entity.ProjectType;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.ProjectMapper;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.service.ProjectMstService;

@Service
@Transactional(readOnly = true)
public class ProjectMstServiceImpl implements ProjectMstService {

    private static final String ACTIVE = "Y";
    private static final String INACTIVE = "N";

    private final ProjectMstRepository projectRepository;
    private final CellMasterRepository cellMasterRepository;
    private final ProjectMapper projectMapper;

    public ProjectMstServiceImpl(
            ProjectMstRepository projectRepository,
            CellMasterRepository cellMasterRepository,
            ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.cellMasterRepository = cellMasterRepository;
        this.projectMapper = projectMapper;
    }

    @Override
    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        String projectName = normalizeName(request.getProjectName());
        ensureUniqueProject(projectName, null, null);

        ProjectMst entity = new ProjectMst();
        mapRequestToEntity(request, entity, projectName);

        return projectMapper.toResponse(projectRepository.save(entity));
    }

    @Override
    @Transactional
    public ProjectResponse update(Long projectId, ProjectRequest request) {
        ProjectMst entity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for id: " + projectId));

        String projectName = normalizeName(request.getProjectName());
        ensureUniqueProject(projectName, entity.getDepartmentRegistrationId(), projectId);
        mapRequestToEntity(request, entity, projectName);

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
            Long departmentRegistrationId,
            Long applicationId) {
        if (applicationId == null) {
            throw new BusinessValidationException("Application id is required to sync project master.");
        }
        if (departmentRegistrationId == null) {
            throw new BusinessValidationException("Department registration id is required to sync project master.");
        }
        if (projectType == null) {
            throw new BusinessValidationException("Project type is required to sync project master.");
        }

        String normalizedProjectName = normalizeName(projectName);
        if (normalizedProjectName == null || normalizedProjectName.isBlank()) {
            throw new BusinessValidationException("Project name is required to sync project master.");
        }

        ProjectMst entity = projectRepository.findFirstByApplicationId(applicationId)
                .orElseGet(() -> projectRepository
                        .findFirstByProjectNameIgnoreCaseAndDepartmentRegistrationId(
                                normalizedProjectName,
                                departmentRegistrationId)
                        .orElseGet(ProjectMst::new));

        entity.setProjectName(normalizedProjectName);
        entity.setProjectType(projectType);
        entity.setProjectScopeType(ProjectScopeType.EXTERNAL);
        entity.setDepartmentRegistrationId(departmentRegistrationId);
        entity.setApplicationId(applicationId);
        entity.setActiveFlag(ACTIVE);

        return projectMapper.toResponse(projectRepository.save(entity));
    }

    private void mapRequestToEntity(ProjectRequest request, ProjectMst entity, String normalizedProjectName) {
        entity.setProjectName(normalizedProjectName);
        entity.setProjectDesc(normalizeDescription(request.getProjectDesc()));
        entity.setProjectType(request.getProjectType());
        entity.setProjectScopeType(resolveProjectScopeType(request.getProjectScopeType(), entity.getApplicationId()));
        entity.setCell(resolveActiveCell(request.getCellId()));
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

    private void ensureUniqueProject(String projectName, Long departmentRegistrationId, Long excludeId) {
        if (projectRepository.existsByProjectNameAndDepartmentRegistrationIdExcludingId(
                projectName,
                departmentRegistrationId,
                excludeId)) {
            throw new DuplicateResourceException("Project already exists with name: " + projectName);
        }
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
