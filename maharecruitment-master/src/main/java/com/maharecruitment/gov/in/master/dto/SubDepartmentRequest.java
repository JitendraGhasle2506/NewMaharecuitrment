package com.maharecruitment.gov.in.master.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubDepartmentRequest {

    @NotBlank(message = "Sub-department name is required")
    @Size(max = 100, message = "Sub-department name must not exceed 100 characters")
    private String subDeptName;

    @NotNull(message = "Department id is required")
    private Long departmentId;
}
