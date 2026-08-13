package com.maharecruitment.gov.in.security.handler;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.common.security.ApplicationCookieService;
import com.maharecruitment.gov.in.common.security.AuthenticationAuditService;

import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler implements LogoutHandler, LogoutSuccessHandler {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CustomLogoutSuccessHandler.class);
    private static final String LOGOUT_AUDIT_RECORDED =
            CustomLogoutSuccessHandler.class.getName() + ".LOGOUT_AUDIT_RECORDED";

    private final ApplicationCookieService applicationCookieService;
    private final AuthenticationAuditService authenticationAuditService;

    public CustomLogoutSuccessHandler(
            ApplicationCookieService applicationCookieService,
            AuthenticationAuditService authenticationAuditService) {
        this.applicationCookieService = applicationCookieService;
        this.authenticationAuditService = authenticationAuditService;
    }

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        String sessionId = request.getSession(false) == null ? null : request.getSession(false).getId();
        recordLogoutAudit(request, authentication, sessionId);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication)
            throws IOException, ServletException {
        if (request.getAttribute(LOGOUT_AUDIT_RECORDED) == null) {
            recordLogoutAudit(request, authentication, request.getRequestedSessionId());
        }
        applicationCookieService.expireRequestCookies(request, response);

        response.sendRedirect(request.getContextPath() + "/login?logout");
    }

    private void recordLogoutAudit(
            HttpServletRequest request,
            Authentication authentication,
            String sessionId) {
        if (authentication == null || sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            authenticationAuditService.recordLogout(
                    authentication.getName(),
                    sessionId,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"),
                    AuthenticationAuditService.REASON_USER_INITIATED,
                    AuthenticationAuditService.SOURCE_WEB);
            request.setAttribute(LOGOUT_AUDIT_RECORDED, Boolean.TRUE);
        } catch (RuntimeException ex) {
            log.error(
                    "Unable to persist logout audit. errorType={}",
                    ex.getClass().getSimpleName(),
                    ex);
        }
    }
}
