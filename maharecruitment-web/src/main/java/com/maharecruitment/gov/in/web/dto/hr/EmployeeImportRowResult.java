package com.maharecruitment.gov.in.web.dto.hr;

public record EmployeeImportRowResult(
        int rowNumber,
        boolean success,
        String action,
        Long employeeId,
        Long userId,
        String employeeCode,
        String fullName,
        String email,
        String recruitmentType,
        String username,
        String temporaryPassword,
        String message) {
}
