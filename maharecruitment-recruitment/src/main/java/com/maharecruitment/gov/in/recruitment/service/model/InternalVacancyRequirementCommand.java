package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyRequirementCommand {

    private Long designationId;
    private String levelCode;
    private Long numberOfVacancy;
}
