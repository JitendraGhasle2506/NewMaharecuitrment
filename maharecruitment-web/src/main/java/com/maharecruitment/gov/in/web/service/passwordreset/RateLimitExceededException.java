package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends PasswordResetException {

    public RateLimitExceededException(long retryAfterSeconds) {
        super(
                "RATE_LIMIT_EXCEEDED",
                "Too many password reset requests. Please try again later.",
                HttpStatus.TOO_MANY_REQUESTS,
                Math.max(1L, retryAfterSeconds));
    }
}
