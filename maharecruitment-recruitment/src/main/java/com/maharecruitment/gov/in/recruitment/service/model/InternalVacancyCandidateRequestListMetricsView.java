package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyCandidateRequestListMetricsView {

    private long requestCount;
    private long totalCandidates;
    private long pendingReviewCandidates;
    private long shortlistedCandidates;
    private long rejectedCandidates;
    private long interviewScheduledCandidates;
    private long feedbackSubmittedCandidates;
}
