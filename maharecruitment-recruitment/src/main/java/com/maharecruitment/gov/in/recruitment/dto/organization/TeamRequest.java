package com.maharecruitment.gov.in.recruitment.dto.organization;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationTeamType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamRequest {

    @NotBlank(message = "Team name is required")
    @Size(max = 150, message = "Team name must not exceed 150 characters")
    private String teamName;

    @NotNull(message = "Team type is required")
    private OrganizationTeamType teamType;

    private Long parentTeamId;

    private Long projectId;

    @NotNull(message = "Cell is required")
    private Long cellId;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder = 0;

    private OrganizationRecordStatus status = OrganizationRecordStatus.ACTIVE;
}
