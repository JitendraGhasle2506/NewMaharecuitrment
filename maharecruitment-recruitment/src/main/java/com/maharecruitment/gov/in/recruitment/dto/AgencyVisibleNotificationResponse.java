package com.maharecruitment.gov.in.recruitment.dto;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyVisibleNotificationResponse {

    private Long recruitmentNotificationId;

    private String requestId;

    private Long departmentRegistrationId;

    private Long departmentProjectApplicationId;

    private Long projectId;

    private String projectName;

    private Integer releasedRank;

    private LocalDateTime notifiedAt;

    private AgencyNotificationTrackingStatus trackingStatus;

    private RecruitmentNotificationStatus notificationStatus;
}

