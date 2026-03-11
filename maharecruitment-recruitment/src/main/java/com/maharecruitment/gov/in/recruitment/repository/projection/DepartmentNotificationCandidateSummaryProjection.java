package com.maharecruitment.gov.in.recruitment.repository.projection;

import java.time.LocalDateTime;

public interface DepartmentNotificationCandidateSummaryProjection {

    Long getRecruitmentNotificationId();

    String getRequestId();

    Long getDepartmentProjectApplicationId();

    String getProjectName();

    Long getTotalCandidates();

    Long getPendingCandidates();

    Long getShortlistedCandidates();

    Long getRejectedCandidates();

    LocalDateTime getLatestSubmittedAt();
}
