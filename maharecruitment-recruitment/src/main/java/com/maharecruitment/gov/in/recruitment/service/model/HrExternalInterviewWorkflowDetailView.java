package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrExternalInterviewWorkflowDetailView {

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
    private String finalDecisionStatus;
    private LocalDateTime interviewDateTime;
    private String interviewTimeSlot;
    private String interviewLink;
    private String agencyName;
    private boolean panelAssigned;
    private LocalDateTime panelAssignedAt;
    private List<InternalVacancyLevelTwoPanelMemberView> panelMembers;
    private int panelFeedbackSubmittedCount;
    private List<InternalVacancyLevelTwoPanelFeedbackView> panelFeedbacks;

    // Computed score fields (populated when panelFeedbackSubmittedCount >= 2)
    private int totalMarksAwarded;
    private int totalMarksPossible;
    private double averageScorePercentage;  // 0.0 – 100.0
    private String computedDecision;        // "SELECTED", "REJECTED", or null
}
