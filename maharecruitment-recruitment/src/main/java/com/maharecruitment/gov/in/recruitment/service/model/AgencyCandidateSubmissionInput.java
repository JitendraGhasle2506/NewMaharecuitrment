package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyCandidateSubmissionInput {

    private String candidateName;

    private String email;

    private String mobile;

    private String candidateEducation;

    private BigDecimal totalExperience;

    private BigDecimal relevantExperience;

    private String joiningTime;

    private String resumeOriginalName;

    private String resumeFilePath;

    private String resumeFileType;

    private Long resumeFileSize;
}
