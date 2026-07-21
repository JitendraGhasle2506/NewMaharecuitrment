package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class PasswordReuseException extends PasswordResetException {

    public PasswordReuseException() {
        super("PASSWORD_REUSED", "New password must be different from the current password.", HttpStatus.BAD_REQUEST);
    }
}
