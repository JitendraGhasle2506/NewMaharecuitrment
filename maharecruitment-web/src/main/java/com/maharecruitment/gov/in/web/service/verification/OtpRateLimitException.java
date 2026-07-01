package com.maharecruitment.gov.in.web.service.verification;

public class OtpRateLimitException extends OtpVerificationException {

    public OtpRateLimitException(String detailMessage, long retryAfterSeconds) {
        super(
                OtpFailureReason.RATE_LIMITED,
                detailMessage,
                OtpVerificationResult.rateLimited(retryAfterSeconds));
    }

    public long getRetryAfterSeconds() {
        return getResult().retryAfterSeconds();
    }
}
