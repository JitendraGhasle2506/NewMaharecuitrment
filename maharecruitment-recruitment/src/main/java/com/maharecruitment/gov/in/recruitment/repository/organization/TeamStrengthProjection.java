package com.maharecruitment.gov.in.recruitment.repository.organization;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationTeamType;

public interface TeamStrengthProjection {

    Long getTeamId();

    String getTeamName();

    OrganizationTeamType getTeamType();

    Long getTotalPositions();

    Long getFilledPositions();

    Long getVacantPositions();
}
