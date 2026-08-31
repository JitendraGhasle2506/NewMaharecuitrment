package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyAssignedInterviewAuthorityView {

    private Long userId;
    private Long employeeId;
    private String name;
    private String designation;
}
