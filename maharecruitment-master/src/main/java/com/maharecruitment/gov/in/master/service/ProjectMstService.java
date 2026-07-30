package com.maharecruitment.gov.in.master.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.ProjectRequest;
import com.maharecruitment.gov.in.master.dto.ProjectResponse;
import com.maharecruitment.gov.in.master.entity.ProjectType;

public interface ProjectMstService {

    ProjectResponse create(ProjectRequest request);

    ProjectResponse update(Long projectId, ProjectRequest request);

    ProjectResponse getById(Long projectId);

    Page<ProjectResponse> getAll(boolean includeInactive, Pageable pageable);

    Page<ProjectResponse> getAll(Long cellId, boolean includeInactive, Pageable pageable);

    void softDelete(Long projectId);

    void restore(Long projectId);

    ProjectResponse upsertFromDepartmentApplication(
            String projectName,
            ProjectType projectType,
            Long departmentRegistrationId,
            Long applicationId);
}
