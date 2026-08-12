package com.maharecruitment.gov.in.web.service.verification;

public class OtpRateLimitException extends OtpVerificationException {

    private final String responseCode;

    public OtpRateLimitException(String detailMessage, long retryAfterSeconds) {
        this(detailMessage, retryAfterSeconds, OtpResponseCodes.OTP_RATE_LIMITED);
    }

    public OtpRateLimitException(String detailMessage, long retryAfterSeconds, String responseCode) {
        super(
                OtpFailureReason.RATE_LIMITED,
                detailMessage,
                OtpVerificationResult.rateLimited(retryAfterSeconds));
        this.responseCode = responseCode;
    }

    public long getRetryAfterSeconds() {
        return getResult().retryAfterSeconds();
    }

    public String getResponseCode() {
        return responseCode;
    }
}
