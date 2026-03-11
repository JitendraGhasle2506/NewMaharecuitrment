package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignationVacancyInput {

    private Long designationId;
    private String levelCode;
    private Long numberOfVacancy;
    private String jobDescription;
}
