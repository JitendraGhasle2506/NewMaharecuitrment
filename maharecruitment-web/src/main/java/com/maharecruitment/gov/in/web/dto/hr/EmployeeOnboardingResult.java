package com.maharecruitment.gov.in.web.dto.hr;

public record EmployeeOnboardingResult(
        Long userId,
        String username,
        String temporaryPassword,
        String notificationWarning) {
}
