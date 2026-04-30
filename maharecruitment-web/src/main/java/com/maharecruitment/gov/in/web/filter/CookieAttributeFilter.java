package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;
import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maharecruitment.gov.in.common.security.CookieSecurityProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CookieAttributeFilter extends OncePerRequestFilter {

    private final CookieSecurityProperties cookieSecurityProperties;
    private final String sameSitePolicy;

    public CookieAttributeFilter(CookieSecurityProperties cookieSecurityProperties) {
        this.cookieSecurityProperties = cookieSecurityProperties;
        this.sameSitePolicy = normalizeSameSite(cookieSecurityProperties.getSameSite());
        if ("None".equals(this.sameSitePolicy) && !cookieSecurityProperties.isSecure()) {
            throw new IllegalStateException(
                    "app.security.cookie.secure must be true when app.security.cookie.same-site is None.");
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, new CookieAttributeResponseWrapper(response));
    }

    private String normalizeSetCookieHeader(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return headerValue;
        }

        String normalizedHeader = headerValue;
        String lowerCaseHeader = headerValue.toLowerCase(Locale.ROOT);

        if (cookieSecurityProperties.isSecure() && !lowerCaseHeader.contains("; secure")) {
            normalizedHeader += "; Secure";
        }

        if (!lowerCaseHeader.contains("; samesite=")) {
            normalizedHeader += "; SameSite=" + sameSitePolicy;
        }

        return normalizedHeader;
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

    private final class CookieAttributeResponseWrapper extends HttpServletResponseWrapper {

        private CookieAttributeResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addCookie(Cookie cookie) {
            if (cookie == null) {
                return;
            }

            cookie.setSecure(cookieSecurityProperties.isSecure());
            cookie.setAttribute("SameSite", sameSitePolicy);
            super.addCookie(cookie);
        }

        @Override
        public void addHeader(String name, String value) {
            if (isSetCookieHeader(name)) {
                super.addHeader(name, normalizeSetCookieHeader(value));
                return;
            }
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            if (isSetCookieHeader(name)) {
                super.setHeader(name, normalizeSetCookieHeader(value));
                return;
            }
            super.setHeader(name, value);
        }

        private boolean isSetCookieHeader(String name) {
            return "Set-Cookie".equalsIgnoreCase(name);
        }
    }
}
