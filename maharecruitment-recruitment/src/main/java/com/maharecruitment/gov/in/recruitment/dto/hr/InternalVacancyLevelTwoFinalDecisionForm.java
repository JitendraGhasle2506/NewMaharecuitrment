package com.maharecruitment.gov.in.recruitment.dto.hr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalVacancyLevelTwoFinalDecisionForm {

    @NotBlank(message = "Final decision is required.")
    private String finalDecision;

    @Size(max = 1000, message = "Decision remarks must not exceed 1000 characters.")
    private String decisionRemarks;
}
