package com.maharecruitment.gov.in.department.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentProjectApplicationSummaryView {

    private Long departmentProjectApplicationId;
    private String requestId;
    private String projectName;
    private String projectCode;
    private DepartmentApplicationType applicationType;
    private DepartmentApplicationStatus applicationStatus;
    private BigDecimal totalEstimatedCost;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
