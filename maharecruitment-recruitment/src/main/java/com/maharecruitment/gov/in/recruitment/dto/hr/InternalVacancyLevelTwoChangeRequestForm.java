package com.maharecruitment.gov.in.recruitment.dto.hr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalVacancyLevelTwoChangeRequestForm {

    @NotBlank(message = "Reason is required.")
    @Size(max = 1000, message = "Reason must be at most 1000 characters.")
    private String changeReason;
}
