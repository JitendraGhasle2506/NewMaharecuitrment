package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancySubmittedCandidateView {

    private Long recruitmentNotificationId;
    private String requestId;
    private String projectName;
    private Long recruitmentInterviewDetailId;
    private Long agencyId;
    private String agencyName;
    private Long designationVacancyId;
    private String designationName;
    private String levelCode;
    private String candidateName;
    private String candidateEmail;
    private String candidateMobile;
    private String candidateEducation;
    private BigDecimal totalExperience;
    private BigDecimal relevantExperience;
    private String joiningTime;
    private String resumeOriginalName;
    private String resumeFilePath;
    private RecruitmentCandidateStatus candidateStatus;
    private String departmentShortlistRemarks;
    private LocalDateTime submittedAt;
    private LocalDateTime interviewDateTime;
    private String interviewTimeSlot;
    private String interviewLink;
    private Boolean interviewChangeRequested;
    private LocalDateTime interviewChangeRequestedAt;
    private Boolean assessmentSubmitted;
    private BigDecimal averagePanelScore;
    private Long submittedAssessmentCount;
    private String finalDecisionStatus;
    private String finalDecisionRemarks;
    private LocalDateTime finalDecisionAt;

    public String getCandidateStatusLabel() {
        if (candidateStatus == null) {
            return "-";
        }
        return switch (candidateStatus) {
            case SHORTLISTED_BY_DEPARTMENT -> "SHORTLISTED BY INTERVIEW AUTHORITY";
            case REJECTED_BY_DEPARTMENT -> "REJECTED BY INTERVIEW AUTHORITY";
            default -> candidateStatus.name().replace('_', ' ');
        };
    }

    public String getCandidateStatusCssClass() {
        if (candidateStatus == null) {
            return "status-neutral";
        }
        return switch (candidateStatus) {
            case SHORTLISTED_BY_DEPARTMENT -> "status-shortlisted";
            case REJECTED_BY_DEPARTMENT -> "status-rejected";
            case INTERVIEW_SCHEDULED_BY_AGENCY, INTERVIEW_REQUEST_SENT_BY_AGENCY -> "status-interview";
            default -> "status-submitted";
        };
    }
}
