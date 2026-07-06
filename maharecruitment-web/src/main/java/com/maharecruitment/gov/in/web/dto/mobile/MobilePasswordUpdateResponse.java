package com.maharecruitment.gov.in.web.dto.mobile;

public record MobilePasswordUpdateResponse(
        boolean success,
        String message,
        Long userId,
        Long employeeId) {
}
