package com.maharecruitment.gov.in.master.dto;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.entity.ProjectType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectResponse {

    private Long projectId;
    private String projectName;
    private String projectDesc;
    private ProjectType projectType;
    private ProjectScopeType projectScopeType;
    private Long departmentRegistrationId;
    private Long applicationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
