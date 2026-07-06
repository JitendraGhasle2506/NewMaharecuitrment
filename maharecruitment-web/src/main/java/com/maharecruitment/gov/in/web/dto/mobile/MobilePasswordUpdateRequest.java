package com.maharecruitment.gov.in.web.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MobilePasswordUpdateRequest(
        @NotNull(message = "Employee ID is required.")
        Long employeeId,

        @NotBlank(message = "Current password is required.")
        String currentPassword,

        @NotBlank(message = "New password is required.")
        String newPassword,

        @NotBlank(message = "Confirm password is required.")
        String confirmPassword) {
}
