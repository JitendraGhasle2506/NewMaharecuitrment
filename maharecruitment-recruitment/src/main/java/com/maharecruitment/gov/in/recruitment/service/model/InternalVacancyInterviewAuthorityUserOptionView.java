package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyInterviewAuthorityUserOptionView {

    private Long userId;
    private String name;
    private String email;
    private String mobileNo;
    private String displayLabel;
}
