package com.maharecruitment.gov.in.department.service.model;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrAgencyRankMappingView {

    private Long departmentId;

    private String departmentName;

    private Long subDepartmentId;

    private String subDepartmentName;

    private Long departmentProjectApplicationId;

    private String requestId;

    private String projectName;

    private Long recruitmentNotificationId;

    private RecruitmentNotificationStatus recruitmentNotificationStatus;

    private boolean recruitmentNotificationAvailable;

    private List<HrAgencyOptionView> agencyOptions;

    private List<HrAssignedAgencyRankView> assignedAgencyRanks;
}

