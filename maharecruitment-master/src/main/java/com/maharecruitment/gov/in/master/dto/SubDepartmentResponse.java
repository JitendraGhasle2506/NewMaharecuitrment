package com.maharecruitment.gov.in.master.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubDepartmentResponse {

    private Long subDeptId;
    private String subDeptName;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
