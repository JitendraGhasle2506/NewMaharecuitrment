package com.maharecruitment.gov.in.web.dto.verification;

public record VerificationResponse(
        String message,
        boolean verified,
        String purpose,
        VerificationChannel channel) {
}
