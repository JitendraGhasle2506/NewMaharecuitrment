package com.maharecruitment.gov.in.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptAuthenticationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginAttemptAuthenticationListener.class);

    private final LoginAttemptBucketService loginAttemptBucketService;

    public LoginAttemptAuthenticationListener(LoginAttemptBucketService loginAttemptBucketService) {
        this.loginAttemptBucketService = loginAttemptBucketService;
    }

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        if (event == null || event.getAuthentication() == null) {
            return;
        }

        try {
            LoginAttemptBucketService.LoginAttemptResult result = loginAttemptBucketService.recordFailure(
                    event.getAuthentication().getName());
            if (result.isBlocked()) {
                LOGGER.warn(
                        "User login blocked after consecutive authentication failures. userId={}, blockedUntil={}",
                        result.userId(),
                        result.blockedUntil());
            }
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "Unable to update the login-attempt bucket. errorType={}",
                    ex.getClass().getSimpleName(),
                    ex);
        }
    }
}
