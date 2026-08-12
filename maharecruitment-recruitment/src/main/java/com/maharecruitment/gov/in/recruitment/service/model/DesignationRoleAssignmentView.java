package com.maharecruitment.gov.in.recruitment.service.model;

import java.util.List;

public record DesignationRoleAssignmentView(
        Long designationId,
        String designationName,
        String category,
        String configuredRoleName,
        boolean configuredRoleAvailable,
        long activeEmployeeCount,
        long linkedUserCount,
        long assignedUserCount,
        long pendingUserCount,
        List<DesignationEmployeeRoleView> employees) {
}
