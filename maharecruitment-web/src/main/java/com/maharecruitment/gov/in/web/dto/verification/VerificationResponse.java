package com.maharecruitment.gov.in.web.dto.verification;

public record VerificationResponse(
        String message,
        boolean verified,
        String purpose,
        VerificationChannel channel,
        int remainingAttempts,
        boolean captchaRequired,
        String captchaId,
        String captchaQuestion,
        String lockedUntil,
        long lockSecondsRemaining,
        int remainingResends,
        long retryAfterSeconds,
        int expirySeconds) {

    public VerificationResponse(
            String message,
            boolean verified,
            String purpose,
            VerificationChannel channel) {
        this(message, verified, purpose, channel, 0, false, null, null, null, 0, 0, 0, 0);
    }
}
