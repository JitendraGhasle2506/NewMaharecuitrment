package com.maharecruitment.gov.in.web.service.mobile;

public class MobileTokenValidationException extends RuntimeException {

    public MobileTokenValidationException(String message) {
        super(message);
    }

    public MobileTokenValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
