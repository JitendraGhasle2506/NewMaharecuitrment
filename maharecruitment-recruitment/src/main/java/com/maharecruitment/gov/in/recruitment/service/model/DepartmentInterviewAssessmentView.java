package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentInterviewAssessmentView {

    private Long recruitmentAssessmentFeedbackId;
    private String interviewAuthority;
    private String candidateName;
    private LocalDateTime interviewDateTime;
    private String mobile;
    private String designationName;
    private String levelCode;
    private String email;
    private String alternateEmail;
    private String qualification;
    private BigDecimal totalExperience;
    private Integer communicationSkillMarks;
    private Integer technicalSkillMarks;
    private Integer relevantExperienceMarks;
    private String interviewerGrade;
    private String recommendationStatus;
    private String assessmentRemarks;
    private String finalRemarks;
    private LocalDateTime submittedAt;
    private List<DepartmentInterviewAssessmentPanelMemberView> panelMembers;

    @Getter
    @Builder
    public static class DepartmentInterviewAssessmentPanelMemberView {
        private String panelMemberName;
        private String panelMemberDesignation;
    }
}
