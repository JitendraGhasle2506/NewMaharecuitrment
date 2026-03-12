package com.maharecruitment.gov.in.recruitment.repository.projection;

import java.time.LocalDateTime;

public interface AgencySelectedCandidateProjectSummaryProjection {

    Long getRecruitmentNotificationId();

    String getRequestId();

    String getProjectName();

    Long getSelectedCandidatesCount();

    LocalDateTime getLatestDecisionAt();
}
