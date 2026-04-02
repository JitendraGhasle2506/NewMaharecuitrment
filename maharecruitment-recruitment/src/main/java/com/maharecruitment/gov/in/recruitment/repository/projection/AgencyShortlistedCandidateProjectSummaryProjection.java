package com.maharecruitment.gov.in.recruitment.repository.projection;

import java.time.LocalDateTime;

public interface AgencyShortlistedCandidateProjectSummaryProjection {

    Long getRecruitmentNotificationId();

    String getRequestId();

    String getProjectName();

    Long getShortlistedCandidatesCount();

    LocalDateTime getLatestShortlistedAt();
}
