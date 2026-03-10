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

    Page<ProjectResponse> getAll(Pageable pageable);

    void delete(Long projectId);

    ProjectResponse upsertFromDepartmentApplication(
            String projectName,
            ProjectType projectType,
            Long departmentRegistrationId,
            Long applicationId);
}
