package com.maharecruitment.gov.in.recruitment.dto.organization;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationTeamType;

public record EmployeeTeamAssignmentResponse(
        Long mappingId,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String designationName,
        String employeeStatus,
        Long teamId,
        String teamName,
        OrganizationTeamType teamType,
        Long cellId,
        String cellName,
        String wingName) {
}
