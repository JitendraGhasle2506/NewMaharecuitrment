package com.maharecruitment.gov.in.security.handler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

import com.maharecruitment.gov.in.common.security.ApplicationCookieService;
import com.maharecruitment.gov.in.common.security.AuthenticationAuditService;

class CustomLogoutSuccessHandlerTest {

    @Test
    void recordsLogoutBeforeSessionIsInvalidated() throws Exception {
        ApplicationCookieService cookieService = mock(ApplicationCookieService.class);
        AuthenticationAuditService auditService = mock(AuthenticationAuditService.class);
        CustomLogoutSuccessHandler handler = new CustomLogoutSuccessHandler(cookieService, auditService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.10.1.2");
        request.addHeader("User-Agent", "Test Browser");
        String sessionId = request.getSession().getId();
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("user@example.com", null, "ROLE_EMPLOYEE");

        handler.logout(request, response, authentication);
        handler.onLogoutSuccess(request, response, authentication);

        verify(auditService).recordLogout(
                eq("user@example.com"),
                eq(sessionId),
                eq("10.10.1.2"),
                eq("Test Browser"),
                eq(AuthenticationAuditService.REASON_USER_INITIATED),
                eq(AuthenticationAuditService.SOURCE_WEB));
        verify(cookieService).expireRequestCookies(request, response);
    }
}
