package com.maharecruitment.gov.in.recruitment.dto.organization;

import jakarta.validation.constraints.NotNull;

public record EmployeeTeamAssignmentRequest(
        @NotNull(message = "Team is required") Long teamId) {
}
