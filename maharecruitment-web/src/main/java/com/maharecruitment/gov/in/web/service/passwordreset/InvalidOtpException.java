package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends PasswordResetException {

    public InvalidOtpException(int remainingAttempts) {
        super(
                "INVALID_OTP",
                "Invalid OTP. Remaining attempts: " + Math.max(0, remainingAttempts) + ".",
                HttpStatus.BAD_REQUEST);
    }
}
