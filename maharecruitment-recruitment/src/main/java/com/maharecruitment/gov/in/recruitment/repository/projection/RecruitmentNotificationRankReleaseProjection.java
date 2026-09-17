package com.maharecruitment.gov.in.recruitment.repository.projection;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;

public interface RecruitmentNotificationRankReleaseProjection {

    Long getRecruitmentNotificationId();

    String getRequestId();

    Long getDepartmentProjectApplicationId();

    RecruitmentNotificationStatus getStatus();

    LocalDateTime getCreatedDateTime();

    String getProjectName();
}
