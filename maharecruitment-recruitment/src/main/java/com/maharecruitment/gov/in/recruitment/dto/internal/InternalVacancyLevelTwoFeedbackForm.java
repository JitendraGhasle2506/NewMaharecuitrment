package com.maharecruitment.gov.in.recruitment.dto.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalVacancyLevelTwoFeedbackForm {

    @NotNull(message = "Communication skill marks are required.")
    @Min(value = 0, message = "Communication skill marks must be between 0 and 5.")
    @Max(value = 5, message = "Communication skill marks must be between 0 and 5.")
    private Integer communicationSkillMarks;

    @NotNull(message = "Technical skill marks are required.")
    @Min(value = 0, message = "Technical skill marks must be between 0 and 5.")
    @Max(value = 5, message = "Technical skill marks must be between 0 and 5.")
    private Integer technicalSkillMarks;

    @NotNull(message = "Leadership quality marks are required.")
    @Min(value = 0, message = "Leadership quality marks must be between 0 and 5.")
    @Max(value = 5, message = "Leadership quality marks must be between 0 and 5.")
    private Integer leadershipQualityMarks;

    @NotNull(message = "Relevant experience marks are required.")
    @Min(value = 0, message = "Relevant experience marks must be between 0 and 5.")
    @Max(value = 5, message = "Relevant experience marks must be between 0 and 5.")
    private Integer relevantExperienceMarks;

    @Size(max = 1000, message = "Assessment remarks must not exceed 1000 characters.")
    private String assessmentRemarks;

    @Size(max = 1000, message = "Final remarks must not exceed 1000 characters.")
    private String finalRemarks;
}
