package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentInterviewWorkflowDetailView {

    private Long recruitmentNotificationId;
    private Long departmentProjectApplicationId;
    private Long recruitmentInterviewDetailId;
    private Long designationVacancyId;
    private String requestId;
    private String projectName;
    private String agencyName;
    private String candidateName;
    private String candidateEmail;
    private String candidateMobile;
    private String candidateEducation;
    private String designationName;
    private String levelCode;
    private BigDecimal totalExperience;
    private BigDecimal relevantExperience;
    private String joiningTime;
    private RecruitmentCandidateStatus candidateStatus;
    private LocalDateTime interviewDateTime;
    private String interviewTimeSlot;
    private String interviewLink;
    private String interviewRemarks;
    private Boolean interviewChangeRequested;
    private String interviewChangeReason;
    private LocalDateTime interviewChangeRequestedAt;
    private Boolean assessmentSubmitted;
    private String finalDecisionStatus;
    private String finalDecisionRemarks;
    private LocalDateTime finalDecisionAt;
    private boolean onboardingCompleted;
    private LocalDateTime onboardedAt;
    private Long vacancyCount;
    private Long filledVacancyCount;
    private Long remainingVacancyCount;
    private String interviewAuthority;
    private List<InternalVacancyAssignedInterviewAuthorityView> interviewAuthorities;
    private boolean panelAssigned;
    private List<InternalVacancyLevelTwoPanelMemberView> panelMembers;
    private int panelFeedbackSubmittedCount;
    private List<InternalVacancyLevelTwoPanelFeedbackView> panelFeedbacks;
    private DepartmentInterviewAssessmentView assessment;

    // MahaIT HR interview data — populated only when interviewAuthority = "MAHAIT_HR"
    private int hrPanelFeedbackCount;
    private List<InternalVacancyLevelTwoPanelFeedbackView> hrPanelFeedbacks;
    private String hrFinalDecisionStatus;       // "SELECTED", "REJECTED", or null
    private String hrFinalDecisionRemarks;
    private double hrAverageScorePercentage;    // 0.0 – 100.0
}
