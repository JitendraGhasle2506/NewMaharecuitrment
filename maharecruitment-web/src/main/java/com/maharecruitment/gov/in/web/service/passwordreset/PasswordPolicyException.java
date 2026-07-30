package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class PasswordPolicyException extends PasswordResetException {

    public PasswordPolicyException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
