package com.maharecruitment.gov.in.department.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrAgencyOptionView {

    private Long agencyId;

    private String agencyName;

    private String officialEmail;
}

