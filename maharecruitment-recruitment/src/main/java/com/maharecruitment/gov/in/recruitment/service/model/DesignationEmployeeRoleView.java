package com.maharecruitment.gov.in.recruitment.service.model;

import java.util.List;

public record DesignationEmployeeRoleView(
        Long employeeId,
        String employeeCode,
        String employeeName,
        Long userId,
        boolean userActive,
        List<String> currentRoleNames,
        boolean configuredRoleAssigned) {
}
