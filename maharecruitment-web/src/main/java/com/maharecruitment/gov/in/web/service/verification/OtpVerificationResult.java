package com.maharecruitment.gov.in.web.service.verification;

import java.time.Instant;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

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
        int expirySeconds,
        VerificationChannel deliveryChannel,
        String maskedDestination,
        int resendAvailableInSeconds) {

    public static OtpVerificationResult sent(int remainingResends, int expirySeconds) {
        return sent(null, null, remainingResends, expirySeconds, 0);
    }

    public static OtpVerificationResult sent(
            VerificationChannel deliveryChannel,
            String maskedDestination,
            int remainingResends,
            int expirySeconds,
            int resendAvailableInSeconds) {
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
                Math.max(1, expirySeconds),
                deliveryChannel,
                maskedDestination,
                Math.max(0, resendAvailableInSeconds));
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
                0,
                null,
                null,
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
                0,
                null,
                null,
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
                0,
                null,
                null,
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
                0,
                null,
                null,
                0);
    }
}
