package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencySubmittedCandidateView {

    private Long recruitmentInterviewDetailId;

    private String candidateName;

    private String candidateEmail;

    private String candidateMobile;

    private String candidateEducation;

    private BigDecimal totalExperience;

    private BigDecimal relevantExperience;

    private String joiningTime;

    private Long vacancyId;

    private String designationName;

    private String levelCode;

    private RecruitmentCandidateStatus candidateStatus;

    private String resumeOriginalName;

    private String resumeFilePath;

    private LocalDateTime interviewDateTime;

    private String interviewTimeSlot;

    private String interviewLink;

    private LocalDateTime createdDateTime;
}
