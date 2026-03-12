package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentShortlistingProjectView {

    private Long recruitmentNotificationId;
    private String requestId;
    private Long departmentProjectApplicationId;
    private String projectName;
    private Long totalCandidates;
    private Long pendingCandidates;
    private Long shortlistedCandidates;
    private Long rejectedCandidates;
    private Long assessmentSubmittedCandidates;
    private Long selectedCandidates;
    private LocalDateTime latestSubmittedAt;
}
