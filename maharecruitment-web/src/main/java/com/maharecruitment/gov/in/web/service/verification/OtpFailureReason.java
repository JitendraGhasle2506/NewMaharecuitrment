package com.maharecruitment.gov.in.web.service.verification;

public enum OtpFailureReason {
    RATE_LIMITED,
    NOT_REQUESTED,
    EXPIRED,
    LOCKED,
    REFERENCE_MISMATCH,
    CAPTCHA_REQUIRED,
    CAPTCHA_INVALID,
    INVALID_OTP,
    INVALIDATED
}
