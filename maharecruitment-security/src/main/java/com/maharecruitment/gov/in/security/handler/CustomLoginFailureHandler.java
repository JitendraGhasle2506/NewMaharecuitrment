package com.maharecruitment.gov.in.security.handler;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String loginUrl = request.getContextPath() + "/login";
        if (exception.getMessage().contains("Captcha")) {
            response.sendRedirect(loginUrl + "?captchaError=true");
        } else {
            response.sendRedirect(loginUrl + "?error=true");
        }
    }
}
