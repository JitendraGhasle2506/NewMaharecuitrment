package com.maharecruitment.gov.in.web.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.common.security.CookieSecurityProperties;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;

@Component
public class SessionCookieConfigInitializer implements ServletContextInitializer {

    private final CookieSecurityProperties cookieSecurityProperties;

    public SessionCookieConfigInitializer(CookieSecurityProperties cookieSecurityProperties) {
        this.cookieSecurityProperties = cookieSecurityProperties;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        servletContext.setSessionTrackingModes(java.util.EnumSet.of(SessionTrackingMode.COOKIE));

        SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
        sessionCookieConfig.setHttpOnly(cookieSecurityProperties.isHttpOnly());
        sessionCookieConfig.setSecure(cookieSecurityProperties.isSecure());
    }
}
