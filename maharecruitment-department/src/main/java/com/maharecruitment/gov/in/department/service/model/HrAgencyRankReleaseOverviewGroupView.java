package com.maharecruitment.gov.in.department.service.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrAgencyRankReleaseOverviewGroupView {

    private String requestId;

    private String projectName;

    private Long departmentId;

    private String departmentName;

    private Long subDepartmentId;

    private String subDepartmentName;

    private Long departmentProjectApplicationId;

    private Long latestNotificationId;

    private int notificationCount;

    private int totalReleaseRows;

    private int releasedCount;

    private int pendingCount;

    private boolean applicationContextAvailable;

    private List<HrAgencyRankMappingListRowView> releaseDetails;
}
