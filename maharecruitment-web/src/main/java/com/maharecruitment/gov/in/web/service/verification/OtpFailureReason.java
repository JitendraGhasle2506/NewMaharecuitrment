package com.maharecruitment.gov.in.web.service.verification;

public enum OtpFailureReason {
    RATE_LIMITED,
    NOT_REQUESTED,
    EXPIRED,
    ATTEMPTS_EXCEEDED,
    LOCKED,
    REFERENCE_MISMATCH,
    CAPTCHA_REQUIRED,
    CAPTCHA_INVALID,
    INVALID_OTP,
    ALREADY_USED,
    INVALIDATED
}
