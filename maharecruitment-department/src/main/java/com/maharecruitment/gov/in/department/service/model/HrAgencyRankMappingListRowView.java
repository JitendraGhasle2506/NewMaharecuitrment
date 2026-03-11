package com.maharecruitment.gov.in.department.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrAgencyRankMappingListRowView {

    private Long recruitmentNotificationId;

    private String requestId;

    private String projectName;

    private Long departmentId;

    private String departmentName;

    private Long subDepartmentId;

    private String subDepartmentName;

    private Long departmentProjectApplicationId;

    private Long agencyId;

    private String agencyName;

    private String agencyEmail;

    private Integer rankNumber;

    private LocalDateTime assignedDate;

    private boolean applicationContextAvailable;
}
