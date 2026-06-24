package com.maharecruitment.gov.in.web.service.verification;

import java.time.Instant;

public record OtpVerificationResult(
        boolean verified,
        int remainingAttempts,
        boolean captchaRequired,
        String captchaId,
        String captchaQuestion,
        Instant lockedUntil,
        long lockSecondsRemaining,
        int remainingResends,
        long retryAfterSeconds,
        int expirySeconds) {

    public static OtpVerificationResult sent(int remainingResends, int expirySeconds) {
        return new OtpVerificationResult(
                false,
                0,
                false,
                null,
                null,
                null,
                0,
                Math.max(0, remainingResends),
                0,
                Math.max(1, expirySeconds));
    }

    public static OtpVerificationResult verified(int maxAttempts) {
        return new OtpVerificationResult(
                true,
                Math.max(0, maxAttempts),
                false,
                null,
                null,
                null,
                0,
                0,
                0,
                0);
    }

    public static OtpVerificationResult failed(
            int remainingAttempts,
            boolean captchaRequired,
            String captchaId,
            String captchaQuestion) {
        return new OtpVerificationResult(
                false,
                Math.max(0, remainingAttempts),
                captchaRequired,
                captchaId,
                captchaQuestion,
                null,
                0,
                0,
                0,
                0);
    }

    public static OtpVerificationResult locked(
            Instant lockedUntil,
            long lockSecondsRemaining,
            boolean captchaRequired,
            String captchaId,
            String captchaQuestion) {
        return new OtpVerificationResult(
                false,
                0,
                captchaRequired,
                captchaId,
                captchaQuestion,
                lockedUntil,
                Math.max(1, lockSecondsRemaining),
                0,
                0,
                0);
    }

    public static OtpVerificationResult rateLimited(long retryAfterSeconds) {
        return new OtpVerificationResult(
                false,
                0,
                false,
                null,
                null,
                null,
                0,
                0,
                Math.max(1, retryAfterSeconds),
                0);
    }
}
