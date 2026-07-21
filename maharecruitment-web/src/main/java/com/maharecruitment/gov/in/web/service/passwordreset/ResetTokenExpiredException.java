package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class ResetTokenExpiredException extends PasswordResetException {

    public ResetTokenExpiredException() {
        super("RESET_TOKEN_EXPIRED", "Reset token has expired. Please request a new OTP.", HttpStatus.UNAUTHORIZED);
    }
}
