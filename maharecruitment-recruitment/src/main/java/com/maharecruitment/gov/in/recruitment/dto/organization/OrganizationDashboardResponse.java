package com.maharecruitment.gov.in.recruitment.dto.organization;

import java.util.ArrayList;
import java.util.List;

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
public class OrganizationDashboardResponse {

    private long totalPositions;
    private long filledPositions;
    private long vacantPositions;

    @Builder.Default
    private List<TeamStrengthResponse> teamStrength = new ArrayList<>();
}
