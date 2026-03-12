package com.maharecruitment.gov.in.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentInterviewTimeChangeForm {

    @NotBlank(message = "Reason is required to request interview time change.")
    @Size(max = 1000, message = "Reason must not exceed 1000 characters.")
    private String changeReason;
}
