package com.maharecruitment.gov.in.department.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentInterviewAssessmentPanelMemberForm {

    @Size(max = 150, message = "Panel member name must not exceed 150 characters.")
    private String panelMemberName;

    @Size(max = 150, message = "Panel member designation must not exceed 150 characters.")
    private String panelMemberDesignation;
}
