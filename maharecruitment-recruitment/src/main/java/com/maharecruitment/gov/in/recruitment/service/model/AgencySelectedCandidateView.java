package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencySelectedCandidateView {

    private Long recruitmentNotificationId;
    private String requestId;
    private String projectName;
    private Long recruitmentInterviewDetailId;
    private String candidateName;
    private String candidateEmail;
    private String candidateMobile;
    private String designationName;
    private String levelCode;
    private BigDecimal totalExperience;
    private BigDecimal relevantExperience;
    private String joiningTime;
    private String resumeFilePath;
    private LocalDateTime interviewDateTime;
    private String interviewTimeSlot;
    private String interviewLink;
    private LocalDateTime finalDecisionAt;
    private String finalDecisionRemarks;
    private boolean preOnboardingCompleted;
    private LocalDateTime preOnboardingSubmittedAt;
}
