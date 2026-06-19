package com.maharecruitment.gov.in.recruitment.dto.organization;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
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
public class TeamResponse {

    private Long teamId;
    private String teamName;
    private OrganizationTeamType teamType;
    private Long parentTeamId;
    private String parentTeamName;
    private Long projectId;
    private String projectName;
    private String projectCode;
    private Long cellId;
    private String cellName;
    private String wingName;
    private Integer displayOrder;
    private OrganizationRecordStatus status;
    private long positionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
