package com.maharecruitment.gov.in.web.service.verification;

public class OtpVerificationException extends RuntimeException {

    private final OtpFailureReason reason;
    private final OtpVerificationResult result;

    public OtpVerificationException(OtpFailureReason reason, String detailMessage, OtpVerificationResult result) {
        super(detailMessage);
        this.reason = reason;
        this.result = result;
    }

    public OtpFailureReason getReason() {
        return reason;
    }

    public OtpVerificationResult getResult() {
        return result;
    }
}
