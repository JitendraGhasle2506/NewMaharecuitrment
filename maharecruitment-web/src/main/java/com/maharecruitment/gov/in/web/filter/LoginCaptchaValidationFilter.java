package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maharecruitment.gov.in.web.service.security.LoginCaptchaService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 7)
public class LoginCaptchaValidationFilter extends OncePerRequestFilter {

    private static final String LOGIN_PROCESSING_PATH = "/doLogin";

    private final LoginCaptchaService captchaService;

    public LoginCaptchaValidationFilter(LoginCaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !LOGIN_PROCESSING_PATH.equals(normalizePath(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (captchaService.validateAndConsume(
                session,
                request.getParameter("loginCaptchaId"),
                request.getParameter("loginCaptchaAnswer"))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/login?loginMode=password&captchaError=true");
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }
}
