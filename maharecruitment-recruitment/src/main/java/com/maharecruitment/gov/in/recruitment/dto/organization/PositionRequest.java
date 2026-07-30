package com.maharecruitment.gov.in.recruitment.dto.organization;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PositionRequest {

    @NotBlank(message = "Position name is required")
    @Size(max = 150, message = "Position name must not exceed 150 characters")
    private String positionName;

    @NotNull(message = "Cell is required")
    private Long cellId;

    private Long teamId;

    @NotNull(message = "Designation is required")
    private Long designationId;

    @Size(max = 10, message = "Level code must not exceed 10 characters")
    private String levelCode;

    private Long reportingPositionId;

    private Long employeeId;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder = 0;

    private OrganizationRecordStatus status = OrganizationRecordStatus.ACTIVE;
}
