package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class OtpAttemptsExceededException extends PasswordResetException {

    public OtpAttemptsExceededException() {
        super("MAX_ATTEMPTS_EXCEEDED", "Maximum OTP attempts exceeded. Please request a new OTP.", HttpStatus.TOO_MANY_REQUESTS);
    }
}
