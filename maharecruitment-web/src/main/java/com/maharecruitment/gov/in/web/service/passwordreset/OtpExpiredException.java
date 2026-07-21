package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class OtpExpiredException extends PasswordResetException {

    public OtpExpiredException() {
        super("OTP_EXPIRED", "OTP has expired. Please request a new OTP.", HttpStatus.BAD_REQUEST);
    }
}
