package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyShortlistedCandidateProjectView {

    private Long recruitmentNotificationId;
    private String requestId;
    private String projectName;
    private Long shortlistedCandidatesCount;
    private LocalDateTime latestShortlistedAt;
}
