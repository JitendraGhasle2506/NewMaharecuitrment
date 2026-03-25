package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyLevelTwoWorkflowDetailView {

    private Long recruitmentNotificationId;
    private Long recruitmentInterviewDetailId;
    private String requestId;
    private String projectName;
    private String candidateName;
    private String candidateEmail;
    private String candidateMobile;
    private String candidateEducation;
    private String designationName;
    private String levelCode;
    private String joiningTime;
    private String resumeFilePath;
    private String recommendationStatus;
    private String finalDecisionStatus;
    private LocalDateTime levelTwoInterviewDateTime;
    private String levelTwoInterviewTimeSlot;
    private String levelTwoMeetingLink;
    private String levelTwoRemarks;
    private LocalDateTime levelTwoScheduledAt;
    private boolean timeChangeRequested;
    private String timeChangeReason;
    private LocalDateTime timeChangeRequestedAt;
    private boolean panelAssigned;
    private LocalDateTime panelAssignedAt;
    private DepartmentInterviewAssessmentView assessment;
    private List<InternalVacancyLevelTwoPanelMemberView> panelMembers;
    private int panelFeedbackSubmittedCount;
    private List<InternalVacancyLevelTwoPanelFeedbackView> panelFeedbacks;
    private InternalVacancyLevelTwoWorkflowStatus workflowStatus;
}
