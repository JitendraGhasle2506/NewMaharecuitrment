package com.maharecruitment.gov.in.department.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentInterviewAssessmentForm {

    @Size(max = 255, message = "Interview authority must not exceed 255 characters.")
    private String interviewAuthority;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime interviewDateTime;

    @Size(max = 15, message = "Mobile number must not exceed 15 digits.")
    private String mobile;

    @Size(max = 255, message = "Email must not exceed 255 characters.")
    private String email;

    @Size(max = 255, message = "Alternate email must not exceed 255 characters.")
    private String alternateEmail;

    @Size(max = 255, message = "Qualification must not exceed 255 characters.")
    private String qualification;

    private BigDecimal totalExperience;

    @NotNull(message = "Communication skill marks are required.")
    @Min(value = 0, message = "Communication skill marks must be between 0 and 5.")
    @Max(value = 5, message = "Communication skill marks must be between 0 and 5.")
    private Integer communicationSkillMarks;

    @NotNull(message = "Technical skill marks are required.")
    @Min(value = 0, message = "Technical skill marks must be between 0 and 5.")
    @Max(value = 5, message = "Technical skill marks must be between 0 and 5.")
    private Integer technicalSkillMarks;

    @NotNull(message = "Relevant experience marks are required.")
    @Min(value = 0, message = "Relevant experience marks must be between 0 and 5.")
    @Max(value = 5, message = "Relevant experience marks must be between 0 and 5.")
    private Integer relevantExperienceMarks;

    @NotBlank(message = "Interviewer grade is required.")
    @Size(max = 10, message = "Interviewer grade must not exceed 10 characters.")
    private String interviewerGrade;

    @NotBlank(message = "Recommendation status is required.")
    @Size(max = 30, message = "Recommendation status must not exceed 30 characters.")
    private String recommendationStatus;

    @Size(max = 1000, message = "Assessment remarks must not exceed 1000 characters.")
    private String assessmentRemarks;

    @Size(max = 1000, message = "Final remarks must not exceed 1000 characters.")
    private String finalRemarks;

    @Valid
    @Size(max = 5, message = "Maximum 5 panel members are allowed.")
    private List<DepartmentInterviewAssessmentPanelMemberForm> panelMembers = new ArrayList<>();
}
