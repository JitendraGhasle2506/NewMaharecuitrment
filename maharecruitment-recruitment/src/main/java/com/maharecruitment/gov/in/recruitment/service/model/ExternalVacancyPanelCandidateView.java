package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExternalVacancyPanelCandidateView {

    private Long recruitmentNotificationId;
    private Long recruitmentInterviewDetailId;
    private String requestId;
    private String projectName;
    private String candidateName;
    private String candidateEmail;
    private String candidateMobile;
    private String designationName;
    private String levelCode;
    private String agencyName;
    private LocalDateTime interviewDateTime;
    private String interviewTimeSlot;
    private String interviewSetting;
    private String interviewLink;
    private String finalDecisionStatus;
    private boolean feedbackSubmitted;
    private LocalDateTime feedbackSubmittedAt;
}
