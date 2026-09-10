package com.maharecruitment.gov.in.auth.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;

class LoginAttemptAuthenticationListenerTest {

    @Test
    void recordsBadCredentialEventsUsingOnlyTheLoginIdentifier() {
        LoginAttemptBucketService bucketService = mock(LoginAttemptBucketService.class);
        when(bucketService.recordFailure("user@example.com"))
                .thenReturn(new LoginAttemptBucketService.LoginAttemptResult(42L, 1, 2, null));
        LoginAttemptAuthenticationListener listener = new LoginAttemptAuthenticationListener(bucketService);
        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
                UsernamePasswordAuthenticationToken.unauthenticated("user@example.com", "secret"),
                new BadCredentialsException("Bad credentials"));

        listener.onBadCredentials(event);

        verify(bucketService).recordFailure("user@example.com");
    }
}
