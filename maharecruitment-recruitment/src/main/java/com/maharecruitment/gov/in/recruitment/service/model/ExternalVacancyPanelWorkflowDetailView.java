package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExternalVacancyPanelWorkflowDetailView {

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
    
    private String agencyName;
    
    // Interview fields
    private LocalDateTime interviewDateTime;
    private String interviewTimeSlot;
    private String interviewLink;
    private String interviewerDesignation; // optional depending on panel

    // My feedback summary
    private boolean feedbackSubmitted;
    private LocalDateTime feedbackSubmittedAt;
    private InternalVacancyLevelTwoPanelFeedbackView myFeedback; 
}
