package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentSubmittedCandidateView {

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
    private String finalDecisionStatus;
    private String finalDecisionRemarks;
    private LocalDateTime finalDecisionAt;
}
