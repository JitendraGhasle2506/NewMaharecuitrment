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
                .projectCode(entity.getProjectCode())
                .projectDesc(entity.getProjectDesc())
                .projectType(entity.getProjectType())
                .projectScopeType(entity.getProjectScopeType())
                .departmentId(entity.getDepartmentId())
                .departmentName(entity.getDepartment() != null ? entity.getDepartment().getDepartmentName() : null)
                .subDepartmentId(entity.getSubDepartmentId())
                .subDepartmentName(entity.getSubDepartment() != null
                        ? entity.getSubDepartment().getSubDeptName()
                        : null)
                .applicationId(entity.getApplicationId())
                .cellId(entity.getCell() != null ? entity.getCell().getCellId() : null)
                .wingName(entity.getCell() != null && entity.getCell().getWing() != null
                        ? entity.getCell().getWing().getWingName()
                        : null)
                .cellName(entity.getCell() != null ? entity.getCell().getCellName() : null)
                .activeFlag(entity.getActiveFlag())
                .createdAt(entity.getCreatedDateTime())
                .updatedAt(entity.getUpdatedDateTime())
                .build();
    }
}
