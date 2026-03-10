package com.maharecruitment.gov.in.department.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationActivityView;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrDepartmentApplicationReviewDetailView {

    private Long departmentId;
    private String departmentName;
    private Long subDepartmentId;
    private String subDepartmentName;

    private Long departmentProjectApplicationId;
    private String requestId;
    private String projectName;
    private String projectCode;
    private DepartmentApplicationType applicationType;
    private DepartmentApplicationStatus applicationStatus;
    private BigDecimal totalEstimatedCost;
    private String remarks;
    private String mahaitContact;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private boolean workOrderAvailable;
    private String workOrderOriginalName;

    private boolean hrActionAllowed;

    private List<HrDepartmentApplicationResourceRequirementView> resourceRequirements;
    private List<DepartmentProjectApplicationActivityView> activityTimeline;
}
