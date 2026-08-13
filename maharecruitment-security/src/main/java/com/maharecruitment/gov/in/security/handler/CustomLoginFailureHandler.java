package com.maharecruitment.gov.in.security.handler;

import java.io.IOException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.common.security.AuthenticationAuditService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomLoginFailureHandler.class);

    private final AuthenticationAuditService authenticationAuditService;

    public CustomLoginFailureHandler(AuthenticationAuditService authenticationAuditService) {
        this.authenticationAuditService = authenticationAuditService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        recordFailure(request, exception);

        String loginUrl = request.getContextPath() + "/login";
        if (isCaptchaFailure(exception)) {
            response.sendRedirect(loginUrl + "?captchaError=true&loginMode=password");
        } else {
            response.sendRedirect(loginUrl + "?error=true&loginMode=password");
        }
    }

    private void recordFailure(HttpServletRequest request, AuthenticationException exception) {
        try {
            authenticationAuditService.recordLoginFailure(
                    request.getParameter("username"),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"),
                    AuthenticationAuditService.METHOD_PASSWORD,
                    failureReason(exception),
                    AuthenticationAuditService.SOURCE_WEB);
        } catch (RuntimeException auditException) {
            LOGGER.error(
                    "Unable to persist failed login audit. errorType={}",
                    auditException.getClass().getSimpleName(),
                    auditException);
        }
    }

    private String failureReason(AuthenticationException exception) {
        if (isCaptchaFailure(exception)) {
            return "CAPTCHA_FAILED";
        }
        if (exception instanceof LockedException) {
            return "ACCOUNT_LOCKED";
        }
        if (exception instanceof DisabledException) {
            return "ACCOUNT_DISABLED";
        }
        if (exception instanceof AccountExpiredException) {
            return "ACCOUNT_EXPIRED";
        }
        if (exception instanceof CredentialsExpiredException) {
            return "CREDENTIALS_EXPIRED";
        }
        if (exception instanceof BadCredentialsException) {
            return "BAD_CREDENTIALS";
        }
        return "AUTHENTICATION_FAILED";
    }

    private boolean isCaptchaFailure(AuthenticationException exception) {
        String message = exception == null ? null : exception.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("captcha");
    }
}
