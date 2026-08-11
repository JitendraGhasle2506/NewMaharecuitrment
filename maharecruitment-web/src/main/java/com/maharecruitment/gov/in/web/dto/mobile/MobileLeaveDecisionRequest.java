package com.maharecruitment.gov.in.web.dto.mobile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobileLeaveDecisionRequest(
        @NotNull(message = "Employee ID is required.")
        Long employeeId,

        @Size(max = 500, message = "Remarks cannot exceed 500 characters.")
        String remarks) {
}
