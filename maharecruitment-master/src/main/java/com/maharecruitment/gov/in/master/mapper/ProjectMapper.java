package com.maharecruitment.gov.in.master.mapper;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.ProjectResponse;
import com.maharecruitment.gov.in.master.entity.ProjectMst;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(ProjectMst entity) {
        return ProjectResponse.builder()
                .projectId(entity.getProjectId())
                .projectName(entity.getProjectName())
                .projectDesc(entity.getProjectDesc())
                .projectType(entity.getProjectType())
                .departmentRegistrationId(entity.getDepartmentRegistrationId())
                .applicationId(entity.getApplicationId())
                .createdAt(entity.getCreatedDateTime())
                .updatedAt(entity.getUpdatedDateTime())
                .build();
    }
}
