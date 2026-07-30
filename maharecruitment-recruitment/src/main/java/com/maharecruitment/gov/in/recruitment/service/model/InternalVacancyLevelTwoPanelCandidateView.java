package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyLevelTwoPanelCandidateView {

    private Long recruitmentNotificationId;
    private Long recruitmentInterviewDetailId;
    private String requestId;
    private String projectName;
    private String candidateName;
    private String candidateEmail;
    private String candidateMobile;
    private String designationName;
    private String levelCode;
    private LocalDateTime levelTwoInterviewDateTime;
    private String levelTwoInterviewTimeSlot;
    private String levelTwoMeetingLink;
    private boolean timeChangeRequested;
    private String finalDecisionStatus;
    private boolean feedbackSubmitted;
    private LocalDateTime feedbackSubmittedAt;
    private InternalVacancyLevelTwoWorkflowStatus workflowStatus;
}
