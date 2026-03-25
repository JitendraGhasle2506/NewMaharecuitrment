package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyInternalAssessmentProjectView {

    private Long recruitmentNotificationId;
    private String requestId;
    private String projectName;
    private Long assessmentSubmittedCandidatesCount;
    private Long recommendedCandidatesCount;
    private LocalDateTime latestAssessmentSubmittedAt;
}
