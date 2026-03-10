package com.maharecruitment.gov.in.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentReviewActionRequest {

    @NotBlank(message = "Decision is required.")
    private String decision;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters.")
    private String remarks;
}
