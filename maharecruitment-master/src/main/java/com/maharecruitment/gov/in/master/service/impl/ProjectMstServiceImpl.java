package com.maharecruitment.gov.in.master.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.ProjectRequest;
import com.maharecruitment.gov.in.master.dto.ProjectResponse;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectType;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.ProjectMapper;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.service.ProjectMstService;

@Service
@Transactional(readOnly = true)
public class ProjectMstServiceImpl implements ProjectMstService {

    private final ProjectMstRepository projectRepository;
    private final ProjectMapper projectMapper;

    public ProjectMstServiceImpl(ProjectMstRepository projectRepository, ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    @Override
    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        String projectName = normalizeName(request.getProjectName());
        ensureUniqueProject(projectName, request.getDepartmentRegistrationId(), null);

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
        ensureUniqueProject(projectName, request.getDepartmentRegistrationId(), projectId);
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
    public Page<ProjectResponse> getAll(Pageable pageable) {
        return projectRepository.findAll(pageable).map(projectMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long projectId) {
        ProjectMst entity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for id: " + projectId));
        projectRepository.delete(entity);
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
        entity.setDepartmentRegistrationId(departmentRegistrationId);
        entity.setApplicationId(applicationId);

        return projectMapper.toResponse(projectRepository.save(entity));
    }

    private void mapRequestToEntity(ProjectRequest request, ProjectMst entity, String normalizedProjectName) {
        entity.setProjectName(normalizedProjectName);
        entity.setProjectDesc(normalizeDescription(request.getProjectDesc()));
        entity.setProjectType(request.getProjectType());
        entity.setDepartmentRegistrationId(request.getDepartmentRegistrationId());
        entity.setApplicationId(request.getApplicationId());
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
