package com.maharecruitment.gov.in.security.handler;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.common.security.ApplicationCookieService;

import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ApplicationCookieService applicationCookieService;

    public CustomLogoutSuccessHandler(ApplicationCookieService applicationCookieService) {
        this.applicationCookieService = applicationCookieService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication)
            throws IOException, ServletException {
        applicationCookieService.expireRequestCookies(request, response);

        response.sendRedirect(request.getContextPath() + "/login?logout");
    }
}
