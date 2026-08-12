package com.maharecruitment.gov.in.recruitment.service.model;

public record DesignationRoleAssignmentResult(
        int assignedUsers,
        int alreadyAssignedUsers,
        int missingUserAccounts,
        int inactiveUserAccounts,
        int skippedDesignations) {
}
