package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyLevelTwoFeedbackSubmissionInput {

    private Integer communicationSkillMarks;
    private Integer technicalSkillMarks;
    private Integer leadershipQualityMarks;
    private Integer relevantExperienceMarks;
    private String interviewerGrade;
    private String recommendationStatus;
    private String assessmentRemarks;
    private String finalRemarks;
}
