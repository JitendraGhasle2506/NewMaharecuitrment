package com.maharecruitment.gov.in.web.dto.mobile;

import jakarta.validation.constraints.NotNull;

public record MobileLeaveEmployeeRequest(
        @NotNull(message = "Employee ID is required.")
        Long employeeId) {
}
