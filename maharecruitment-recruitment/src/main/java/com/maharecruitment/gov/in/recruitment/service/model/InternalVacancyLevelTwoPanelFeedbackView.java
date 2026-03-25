package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyLevelTwoPanelFeedbackView {

    private Long feedbackId;
    private Long reviewerUserId;
    private String reviewerName;
    private String reviewerRoleLabel;
    private Integer communicationSkillMarks;
    private Integer technicalSkillMarks;
    private Integer leadershipQualityMarks;
    private Integer relevantExperienceMarks;
    private String interviewerGrade;
    private String recommendationStatus;
    private String assessmentRemarks;
    private String finalRemarks;
    private LocalDateTime submittedAt;
}
