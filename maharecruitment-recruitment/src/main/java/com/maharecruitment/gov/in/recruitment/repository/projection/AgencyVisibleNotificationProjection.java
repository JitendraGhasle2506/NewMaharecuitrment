package com.maharecruitment.gov.in.recruitment.repository.projection;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;

public interface AgencyVisibleNotificationProjection {

    Long getRecruitmentNotificationId();

    String getRequestId();

    Long getDepartmentRegistrationId();

    Long getDepartmentProjectApplicationId();

    Long getProjectId();

    String getProjectName();

    Integer getReleasedRank();

    LocalDateTime getNotifiedAt();

    AgencyNotificationTrackingStatus getTrackingStatus();

    RecruitmentNotificationStatus getNotificationStatus();
}
