package com.maharecruitment.gov.in.master.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentResponse {

    private Long departmentId;
    private String departmentName;
    private List<SubDepartmentSummaryResponse> subDepartments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
