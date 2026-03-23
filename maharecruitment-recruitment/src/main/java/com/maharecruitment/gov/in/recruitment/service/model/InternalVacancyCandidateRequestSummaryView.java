package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyCandidateRequestSummaryView {

    private String requestId;
    private String projectName;
    private Long totalCandidates;
    private Long interviewScheduledCandidates;
    private LocalDateTime latestSubmittedAt;
}
