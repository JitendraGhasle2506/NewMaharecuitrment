package com.maharecruitment.gov.in.recruitment.repository.projection;

public interface InternalVacancyCandidateRequestSummaryMetricsProjection {

    Long getRequestCount();

    Long getTotalCandidates();

    Long getPendingReviewCandidates();

    Long getShortlistedCandidates();

    Long getRejectedCandidates();

    Long getInterviewScheduledCandidates();

    Long getFeedbackSubmittedCandidates();
}
