package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class PasswordResetException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Long retryAfterSeconds;

    public PasswordResetException(String code, String message, HttpStatus status) {
        this(code, message, status, null);
    }

    public PasswordResetException(String code, String message, HttpStatus status, Long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
