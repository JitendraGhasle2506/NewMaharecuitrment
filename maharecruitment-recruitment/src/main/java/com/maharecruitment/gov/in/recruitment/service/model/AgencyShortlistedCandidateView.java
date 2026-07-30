package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyShortlistedCandidateView {

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
    private RecruitmentCandidateStatus candidateStatus;
    private LocalDateTime departmentShortlistedAt;
    private String departmentShortlistRemarks;
    private LocalDateTime interviewDateTime;
    private String interviewTimeSlot;
    private String interviewLink;
    private String interviewRemarks;
    private Boolean assessmentSubmitted;
}
