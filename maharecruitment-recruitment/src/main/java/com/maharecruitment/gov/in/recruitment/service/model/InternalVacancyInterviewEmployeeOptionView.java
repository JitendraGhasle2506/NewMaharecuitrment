package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyInterviewEmployeeOptionView {

    private Long employeeId;
    private String fullName;
    private String employeeCode;
    private String email;
    private String mobile;
    private String designationName;
    private String displayLabel;
}
