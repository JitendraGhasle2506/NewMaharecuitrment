package com.maharecruitment.gov.in.security.handler;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.common.util.CookieUtil;

import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication)
            throws IOException, ServletException {
        boolean secureRequest = CookieUtil.isSecureRequest(request);
        String contextPath = request.getContextPath();
        String cookiePath = (contextPath == null || contextPath.isBlank()) ? "/" : contextPath;

        // Delete all cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                response.addCookie(CookieUtil.deleteCookie(c.getName(), "/", secureRequest));
                if (!"/".equals(cookiePath)) {
                    response.addCookie(CookieUtil.deleteCookie(c.getName(), cookiePath, secureRequest));
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/login?logout");
    }
}
