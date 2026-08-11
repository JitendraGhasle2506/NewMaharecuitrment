package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.LocalDate;

public record MobileCompOffValidationResponse(
        boolean success,
        String message,
        Long employeeId,
        LocalDate workedDate,
        boolean valid) {
}
