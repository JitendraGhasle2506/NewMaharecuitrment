package com.maharecruitment.gov.in.web.interceptor;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.maharecruitment.gov.in.common.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class SessionValidationInterceptor implements HandlerInterceptor {

    private static final String SESSION_USER_KEY = "SESSION_USER";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_USER_KEY) != null) {
            return true;
        }

        SecurityContextHolder.clearContext();
        if (session != null) {
            session.invalidate();
        }
        clearSessionCookie(request, response);

        String redirectUrl = request.getContextPath() + "/login?sessionExpired=true";
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"SESSION_EXPIRED\",\"redirect\":\"" + redirectUrl + "\"}");
            return false;
        }

        response.sendRedirect(redirectUrl);
        return false;
    }

    private void clearSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        String contextPath = request.getContextPath();
        String cookiePath = (contextPath == null || contextPath.isBlank()) ? "/" : contextPath;
        boolean secureRequest = CookieUtil.isSecureRequest(request);

        response.addCookie(CookieUtil.deleteCookie("JSESSIONID", cookiePath, secureRequest));
        response.addCookie(CookieUtil.deleteCookie("JSESSIONID", "/", secureRequest));
    }
}
