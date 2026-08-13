package com.maharecruitment.gov.in.security.handler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import com.maharecruitment.gov.in.common.security.AuthenticationAuditService;

class CustomLoginFailureHandlerTest {

    @Test
    void recordsFailedPasswordLoginAndPreservesExistingRedirect() throws Exception {
        AuthenticationAuditService auditService = mock(AuthenticationAuditService.class);
        CustomLoginFailureHandler handler = new CustomLoginFailureHandler(auditService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/maharecruitment");
        request.setRemoteAddr("10.10.1.4");
        request.addHeader("User-Agent", "Test Browser");
        request.addParameter("username", "user@example.com");
        request.addParameter("password", "must-not-be-audited");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                new BadCredentialsException("Bad credentials"));

        verify(auditService).recordLoginFailure(
                eq("user@example.com"),
                eq("10.10.1.4"),
                eq("Test Browser"),
                eq(AuthenticationAuditService.METHOD_PASSWORD),
                eq("BAD_CREDENTIALS"),
                eq(AuthenticationAuditService.SOURCE_WEB));
        org.assertj.core.api.Assertions.assertThat(response.getRedirectedUrl())
                .isEqualTo("/maharecruitment/login?error=true&loginMode=password");
    }
}
