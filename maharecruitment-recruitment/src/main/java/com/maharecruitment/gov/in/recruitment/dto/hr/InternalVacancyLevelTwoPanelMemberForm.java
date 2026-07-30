package com.maharecruitment.gov.in.recruitment.dto.hr;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalVacancyLevelTwoPanelMemberForm {

    @Size(max = 150, message = "Panel member name must be at most 150 characters.")
    private String panelMemberName;

    @Size(max = 150, message = "Panel member designation must be at most 150 characters.")
    private String panelMemberDesignation;
}

