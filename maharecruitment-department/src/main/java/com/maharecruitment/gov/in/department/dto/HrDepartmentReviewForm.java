package com.maharecruitment.gov.in.department.dto;

import com.maharecruitment.gov.in.department.entity.HrReviewDecision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HrDepartmentReviewForm {

    @NotNull(message = "Decision is required.")
    private HrReviewDecision decision;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters.")
    private String remarks;
}
