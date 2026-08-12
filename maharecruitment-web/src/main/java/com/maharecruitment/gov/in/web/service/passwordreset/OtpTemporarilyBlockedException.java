package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class OtpTemporarilyBlockedException extends PasswordResetException {

    public OtpTemporarilyBlockedException(long retryAfterSeconds) {
        super(
                "OTP_TEMPORARILY_BLOCKED",
                "OTP operations are temporarily blocked. Please try again later.",
                HttpStatus.TOO_MANY_REQUESTS,
                Math.max(1, retryAfterSeconds));
    }
}
