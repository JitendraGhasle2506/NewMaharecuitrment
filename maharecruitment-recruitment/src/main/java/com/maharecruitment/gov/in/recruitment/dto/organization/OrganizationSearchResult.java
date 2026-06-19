package com.maharecruitment.gov.in.recruitment.dto.organization;

import com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus;

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
public class OrganizationSearchResult {

    private String resultType;
    private Long id;
    private String title;
    private String subtitle;
    private Long projectId;
    private String projectName;
    private Long cellId;
    private String cellName;
    private Long teamId;
    private String teamName;
    private String designationName;
    private String levelCode;
    private String levelName;
    private PositionStatus positionStatus;
    private boolean vacant;
}
