package com.maharecruitment.gov.in.recruitment.dto.internal;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalInterviewAssessmentPanelMemberForm {

    @Size(max = 150, message = "Panel member name must not exceed 150 characters.")
    private String panelMemberName;

    @Size(max = 150, message = "Panel member designation must not exceed 150 characters.")
    private String panelMemberDesignation;
}
