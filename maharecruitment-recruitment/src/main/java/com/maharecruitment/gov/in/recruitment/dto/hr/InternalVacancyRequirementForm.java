package com.maharecruitment.gov.in.recruitment.dto.hr;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalVacancyRequirementForm {

    @NotNull(message = "Designation is required.")
    private Long designationId;

    @Size(max = 200, message = "Designation name must not exceed 200 characters.")
    private String designationName;

    @NotBlank(message = "Level is required.")
    @Size(max = 10, message = "Level code must not exceed 10 characters.")
    private String levelCode;

    @Size(max = 100, message = "Level name must not exceed 100 characters.")
    private String levelName;

    @NotNull(message = "Number of vacancies is required.")
    @Min(value = 1, message = "Number of vacancies must be at least 1.")
    private Long numberOfVacancy;
}
