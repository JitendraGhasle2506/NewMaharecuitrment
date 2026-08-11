package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobileLeaveApplyRequest(
        @NotNull(message = "Employee ID is required.")
        Long employeeId,

        @NotBlank(message = "Leave type is required.")
        @Size(max = 100, message = "Leave type cannot exceed 100 characters.")
        String leaveType,

        @NotBlank(message = "Leave category is required.")
        @Size(max = 50, message = "Leave category cannot exceed 50 characters.")
        String leaveCategory,

        @NotNull(message = "Start date is required.")
        LocalDate startDate,

        @NotNull(message = "End date is required.")
        LocalDate endDate,

        LocalDate compOffWorkDate,

        @Size(max = 500, message = "Description cannot exceed 500 characters.")
        String description) {
}
