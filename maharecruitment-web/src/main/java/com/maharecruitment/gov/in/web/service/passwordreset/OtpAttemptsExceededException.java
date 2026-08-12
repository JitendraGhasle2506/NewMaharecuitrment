package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class OtpAttemptsExceededException extends PasswordResetException {

    public OtpAttemptsExceededException() {
        this(null);
    }

    public OtpAttemptsExceededException(Long retryAfterSeconds) {
        super(
                "OTP_ATTEMPTS_EXCEEDED",
                "Maximum OTP verification attempts exceeded. Please request a new OTP.",
                HttpStatus.TOO_MANY_REQUESTS,
                retryAfterSeconds);
    }
}
