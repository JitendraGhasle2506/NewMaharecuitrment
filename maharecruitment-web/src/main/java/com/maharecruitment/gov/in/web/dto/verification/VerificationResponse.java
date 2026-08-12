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
        int expirySeconds,
        String deliveryChannel,
        String maskedDestination,
        int expiresInSeconds,
        int resendAvailableInSeconds,
        boolean success,
        String code) {

    public VerificationResponse(
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
            int expirySeconds,
            String deliveryChannel,
            String maskedDestination,
            int expiresInSeconds,
            int resendAvailableInSeconds) {
        this(
                message,
                verified,
                purpose,
                channel,
                remainingAttempts,
                captchaRequired,
                captchaId,
                captchaQuestion,
                lockedUntil,
                lockSecondsRemaining,
                remainingResends,
                retryAfterSeconds,
                expirySeconds,
                deliveryChannel,
                maskedDestination,
                expiresInSeconds,
                resendAvailableInSeconds,
                verified,
                verified ? "OTP_VERIFIED" : "OTP_REQUEST_REJECTED");
    }

    public VerificationResponse(
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
        this(
                message,
                verified,
                purpose,
                channel,
                remainingAttempts,
                captchaRequired,
                captchaId,
                captchaQuestion,
                lockedUntil,
                lockSecondsRemaining,
                remainingResends,
                retryAfterSeconds,
                expirySeconds,
                channel == null ? null : channel.name(),
                null,
                expirySeconds,
                0,
                verified,
                verified ? "OTP_VERIFIED" : "OTP_REQUEST_REJECTED");
    }

    public VerificationResponse(
            String message,
            boolean verified,
            String purpose,
            VerificationChannel channel) {
        this(message, verified, purpose, channel, 0, false, null, null, null, 0, 0, 0, 0);
    }
}
