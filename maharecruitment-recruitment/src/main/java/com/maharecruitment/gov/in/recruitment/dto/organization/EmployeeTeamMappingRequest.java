package com.maharecruitment.gov.in.recruitment.dto.organization;

import java.time.LocalDate;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeTeamMappingRequest {

    private Long employeeId;

    @NotNull(message = "Team is required")
    private Long teamId;

    private Long positionId;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private OrganizationRecordStatus status = OrganizationRecordStatus.ACTIVE;
}
