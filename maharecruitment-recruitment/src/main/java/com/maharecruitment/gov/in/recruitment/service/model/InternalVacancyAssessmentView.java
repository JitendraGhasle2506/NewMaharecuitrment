package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyAssessmentView {
    private Long assessmentId;
    private String assessorName;
    private String assessorType; // USER or EMPLOYEE
    private BigDecimal technicalScore;
    private BigDecimal communicationScore;
    private BigDecimal leadershipScore;
    private BigDecimal relevantExperienceScore;
    private BigDecimal totalScore;
    private String remarks;
    private String interviewerGrade;
    private String recommendationStatus;
    private LocalDateTime submittedAt;
}
