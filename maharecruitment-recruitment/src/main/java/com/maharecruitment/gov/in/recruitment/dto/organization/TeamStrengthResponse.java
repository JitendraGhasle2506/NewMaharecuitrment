package com.maharecruitment.gov.in.recruitment.dto.organization;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationTeamType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamStrengthResponse {

    private Long teamId;
    private String teamName;
    private OrganizationTeamType teamType;
    private long totalPositions;
    private long filledPositions;
    private long vacantPositions;
}
