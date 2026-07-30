package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentInterviewAssessmentSubmissionInput {

    private String interviewAuthority;
    private LocalDateTime interviewDateTime;
    private String mobile;
    private String email;
    private String alternateEmail;
    private String qualification;
    private BigDecimal totalExperience;
    private Integer communicationSkillMarks;
    private Integer technicalSkillMarks;
    private Integer leadershipQualityMarks;
    private Integer relevantExperienceMarks;
    private String interviewerGrade;
    private String recommendationStatus;
    private String assessmentRemarks;
    private String finalRemarks;

    @Builder.Default
    private List<DepartmentInterviewAssessmentPanelMemberInput> panelMembers = new ArrayList<>();
}
