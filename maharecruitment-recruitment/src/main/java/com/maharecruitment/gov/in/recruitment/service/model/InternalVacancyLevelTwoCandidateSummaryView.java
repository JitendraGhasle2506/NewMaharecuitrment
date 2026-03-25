package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyLevelTwoCandidateSummaryView {

    private Long recruitmentNotificationId;
    private Long recruitmentInterviewDetailId;
    private String requestId;
    private String projectName;
    private String candidateName;
    private String candidateEmail;
    private String candidateMobile;
    private String designationName;
    private String levelCode;
    private String recommendationStatus;
    private LocalDateTime levelTwoInterviewDateTime;
    private String levelTwoInterviewTimeSlot;
    private LocalDateTime levelTwoScheduledAt;
    private boolean panelAssigned;
    private LocalDateTime panelAssignedAt;
    private boolean timeChangeRequested;
    private LocalDateTime timeChangeRequestedAt;
    private String finalDecisionStatus;
    private InternalVacancyLevelTwoWorkflowStatus workflowStatus;
}
