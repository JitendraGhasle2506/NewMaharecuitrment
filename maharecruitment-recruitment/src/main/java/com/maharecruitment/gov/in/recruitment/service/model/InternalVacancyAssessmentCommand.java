package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class InternalVacancyAssessmentCommand {
    private Long recruitmentInterviewDetailId;
    private BigDecimal technicalScore;
    private BigDecimal communicationScore;
    private BigDecimal leadershipScore;
    private BigDecimal relevantExperienceScore;
    private String remarks;
    private String interviewerGrade;
    private String recommendationStatus;
}
