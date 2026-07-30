package com.maharecruitment.gov.in.master.dto;

import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.entity.ProjectType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must not exceed 100 characters")
    private String projectName;

    @NotBlank(message = "Project code is required")
    @Size(max = 30, message = "Project code must not exceed 30 characters")
    private String projectCode;

    @Size(max = 100, message = "Project description must not exceed 100 characters")
    private String projectDesc;

    @NotNull(message = "Project type is required")
    private ProjectType projectType;

    @NotNull(message = "Project scope is required")
    private ProjectScopeType projectScopeType;

    @NotNull(message = "Cell is required")
    private Long cellId;
}
