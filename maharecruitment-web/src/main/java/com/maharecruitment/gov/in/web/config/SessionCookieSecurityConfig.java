package com.maharecruitment.gov.in.web.config;

import java.util.Locale;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;

import com.maharecruitment.gov.in.common.security.CookieSecurityProperties;

import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;

@Configuration
public class SessionCookieSecurityConfig implements ServletContextInitializer {

    private final CookieSecurityProperties cookieSecurityProperties;

    public SessionCookieSecurityConfig(CookieSecurityProperties cookieSecurityProperties) {
        this.cookieSecurityProperties = cookieSecurityProperties;
    }

    @Override
    public void onStartup(ServletContext servletContext) {
        SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
        sessionCookieConfig.setSecure(cookieSecurityProperties.isSecure());
        sessionCookieConfig.setHttpOnly(cookieSecurityProperties.isHttpOnly());
        sessionCookieConfig.setAttribute("SameSite", normalizeSameSite(cookieSecurityProperties.getSameSite()));
    }

    private String normalizeSameSite(String sameSite) {
        if (sameSite == null || sameSite.isBlank()) {
            return "Lax";
        }

        return switch (sameSite.trim().toUpperCase(Locale.ROOT)) {
            case "STRICT" -> "Strict";
            case "LAX" -> "Lax";
            case "NONE" -> "None";
            default -> throw new IllegalStateException(
                    "app.security.cookie.same-site must be Strict, Lax, or None.");
        };
    }
}
