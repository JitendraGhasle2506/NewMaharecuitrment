package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencySelectedCandidateProjectView {

    private Long recruitmentNotificationId;
    private String requestId;
    private String projectName;
    private Long selectedCandidatesCount;
    private LocalDateTime latestDecisionAt;
}
