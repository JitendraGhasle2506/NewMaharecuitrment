package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyRankAssignmentCommand {

    private Long agencyId;

    private Integer rankNumber;
}

