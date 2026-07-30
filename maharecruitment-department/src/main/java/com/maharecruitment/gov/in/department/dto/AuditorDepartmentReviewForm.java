package com.maharecruitment.gov.in.department.dto;

import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditorDepartmentReviewForm {

    @NotNull(message = "Decision is required.")
    private AuditorReviewDecision decision;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters.")
    private String remarks;
}
