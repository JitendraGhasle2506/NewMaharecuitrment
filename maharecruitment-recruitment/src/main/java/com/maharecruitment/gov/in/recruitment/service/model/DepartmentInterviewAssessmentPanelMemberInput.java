package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentInterviewAssessmentPanelMemberInput {

    private String panelMemberName;
    private String panelMemberDesignation;
}
