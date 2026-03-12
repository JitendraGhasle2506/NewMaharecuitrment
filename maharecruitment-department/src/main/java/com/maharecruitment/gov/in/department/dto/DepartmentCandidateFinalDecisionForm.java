package com.maharecruitment.gov.in.department.dto;

import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateFinalDecision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentCandidateFinalDecisionForm {

    @NotNull(message = "Final decision is required.")
    private DepartmentCandidateFinalDecision finalDecision;

    @Size(max = 1000, message = "Decision remarks must not exceed 1000 characters.")
    private String decisionRemarks;
}
