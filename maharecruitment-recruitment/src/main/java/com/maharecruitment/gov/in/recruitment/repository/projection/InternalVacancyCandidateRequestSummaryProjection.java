package com.maharecruitment.gov.in.recruitment.repository.projection;

import java.time.LocalDateTime;

public interface InternalVacancyCandidateRequestSummaryProjection {

    String getRequestId();

    String getProjectName();

    Long getTotalCandidates();

    Long getPendingReviewCandidates();

    Long getShortlistedCandidates();

    Long getRejectedCandidates();

    Long getInterviewScheduledCandidates();

    Long getFeedbackSubmittedCandidates();

    LocalDateTime getLatestSubmittedAt();
}
