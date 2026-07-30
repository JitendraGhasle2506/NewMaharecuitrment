package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class InvalidResetTokenException extends PasswordResetException {

    public InvalidResetTokenException() {
        super("INVALID_RESET_TOKEN", "Reset token is invalid or has already been used.", HttpStatus.UNAUTHORIZED);
    }
}
