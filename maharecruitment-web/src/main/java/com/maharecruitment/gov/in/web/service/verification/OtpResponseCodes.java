package com.maharecruitment.gov.in.web.service.verification;

/** Stable, non-sensitive API codes used by every web OTP client. */
public final class OtpResponseCodes {

    public static final String OTP_SENT = "OTP_SENT";
    public static final String OTP_VERIFIED = "OTP_VERIFIED";
    public static final String INVALID_OTP = "INVALID_OTP";
    public static final String OTP_EXPIRED = "OTP_EXPIRED";
    public static final String OTP_ATTEMPTS_EXCEEDED = "OTP_ATTEMPTS_EXCEEDED";
    public static final String OTP_ALREADY_USED = "OTP_ALREADY_USED";
    public static final String OTP_NOT_FOUND = "OTP_NOT_FOUND";
    public static final String OTP_RESEND_COOLDOWN = "OTP_RESEND_COOLDOWN";
    public static final String OTP_RESEND_LIMIT_EXCEEDED = "OTP_RESEND_LIMIT_EXCEEDED";
    public static final String OTP_TEMPORARILY_BLOCKED = "OTP_TEMPORARILY_BLOCKED";
    public static final String OTP_RATE_LIMITED = "OTP_RATE_LIMITED";
    public static final String OTP_CAPTCHA_REQUIRED = "OTP_CAPTCHA_REQUIRED";
    public static final String OTP_CAPTCHA_INVALID = "OTP_CAPTCHA_INVALID";
    public static final String OTP_REQUEST_REJECTED = "OTP_REQUEST_REJECTED";
    public static final String OTP_DELIVERY_UNAVAILABLE = "OTP_DELIVERY_UNAVAILABLE";

    public static String forFailure(OtpFailureReason reason) {
        if (reason == null) {
            return OTP_REQUEST_REJECTED;
        }
        return switch (reason) {
            case RATE_LIMITED -> OTP_RATE_LIMITED;
            case NOT_REQUESTED, REFERENCE_MISMATCH, INVALIDATED -> OTP_NOT_FOUND;
            case EXPIRED -> OTP_EXPIRED;
            case ATTEMPTS_EXCEEDED -> OTP_ATTEMPTS_EXCEEDED;
            case LOCKED -> OTP_TEMPORARILY_BLOCKED;
            case CAPTCHA_REQUIRED -> OTP_CAPTCHA_REQUIRED;
            case CAPTCHA_INVALID -> OTP_CAPTCHA_INVALID;
            case INVALID_OTP -> INVALID_OTP;
            case ALREADY_USED -> OTP_ALREADY_USED;
        };
    }

    public static String messageFor(OtpFailureReason reason, int remainingAttempts) {
        if (reason == null) {
            return "OTP verification failed. Please try again.";
        }
        return switch (reason) {
            case INVALID_OTP -> remainingAttempts > 0
                    ? "Incorrect OTP. " + remainingAttempts + " attempts remaining."
                    : "Invalid OTP.";
            case EXPIRED -> "OTP has expired. Please request a new OTP.";
            case ATTEMPTS_EXCEEDED ->
                    "Maximum OTP verification attempts exceeded. Please request a new OTP.";
            case LOCKED -> "OTP operations are temporarily blocked. Please try again later.";
            case ALREADY_USED -> "OTP has already been used. Please request a new OTP.";
            case NOT_REQUESTED, REFERENCE_MISMATCH, INVALIDATED ->
                    "OTP was not found or is no longer valid. Please request a new OTP.";
            case CAPTCHA_REQUIRED -> "Complete the CAPTCHA before verifying the OTP.";
            case CAPTCHA_INVALID -> "Invalid CAPTCHA response.";
            case RATE_LIMITED -> "Too many OTP requests. Please try again later.";
        };
    }

    private OtpResponseCodes() {
    }
}
