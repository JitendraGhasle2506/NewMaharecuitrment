package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maharecruitment.gov.in.common.security.CookieSecurityProperties;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CookieAttributeFilter extends OncePerRequestFilter {

    private static final Pattern SECURE_ATTRIBUTE = Pattern.compile("(?i);\\s*Secure\\b");
    private static final Pattern HTTP_ONLY_ATTRIBUTE = Pattern.compile("(?i);\\s*HttpOnly\\b");
    private static final Pattern SAME_SITE_ATTRIBUTE = Pattern.compile("(?i);\\s*SameSite\\s*=\\s*[^;]*");

    private final CookieSecurityProperties cookieSecurityProperties;
    private final TransportSecurityProperties transportSecurityProperties;
    private final String sameSitePolicy;

    public CookieAttributeFilter(
            CookieSecurityProperties cookieSecurityProperties,
            TransportSecurityProperties transportSecurityProperties) {
        this.cookieSecurityProperties = cookieSecurityProperties;
        this.transportSecurityProperties = transportSecurityProperties;
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
        filterChain.doFilter(request, new CookieAttributeResponseWrapper(
                response,
                shouldUseSecureCookie(request)));
    }

    private boolean shouldUseSecureCookie(HttpServletRequest request) {
        return cookieSecurityProperties.isSecure()
                && !transportSecurityProperties.isLoopbackHttpAllowed(request);
    }

    private String normalizeSetCookieHeader(String headerValue, boolean secureCookieRequired) {
        if (headerValue == null || headerValue.isBlank()) {
            return headerValue;
        }

        String normalizedHeader = SAME_SITE_ATTRIBUTE.matcher(headerValue).replaceAll("");

        if (secureCookieRequired && !SECURE_ATTRIBUTE.matcher(normalizedHeader).find()) {
            normalizedHeader += "; Secure";
        }
        if (!secureCookieRequired) {
            normalizedHeader = SECURE_ATTRIBUTE.matcher(normalizedHeader).replaceAll("");
        }

        if (cookieSecurityProperties.isHttpOnly() && !HTTP_ONLY_ATTRIBUTE.matcher(normalizedHeader).find()) {
            normalizedHeader += "; HttpOnly";
        }

        normalizedHeader += "; SameSite=" + sameSitePolicy;

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

        private final boolean secureCookieRequired;

        private CookieAttributeResponseWrapper(HttpServletResponse response, boolean secureCookieRequired) {
            super(response);
            this.secureCookieRequired = secureCookieRequired;
        }

        @Override
        public void addCookie(Cookie cookie) {
            if (cookie == null) {
                return;
            }

            cookie.setSecure(secureCookieRequired);
            cookie.setHttpOnly(cookieSecurityProperties.isHttpOnly());
            cookie.setAttribute("SameSite", sameSitePolicy);
            super.addHeader("Set-Cookie", buildSetCookieHeader(cookie));
        }

        @Override
        public void addHeader(String name, String value) {
            if (isSetCookieHeader(name)) {
                super.addHeader(name, normalizeSetCookieHeader(value, secureCookieRequired));
                return;
            }
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            if (isSetCookieHeader(name)) {
                super.setHeader(name, normalizeSetCookieHeader(value, secureCookieRequired));
                return;
            }
            super.setHeader(name, value);
        }

        private boolean isSetCookieHeader(String name) {
            return "Set-Cookie".equalsIgnoreCase(name);
        }

        private String buildSetCookieHeader(Cookie cookie) {
            StringBuilder header = new StringBuilder();
            header.append(cookie.getName()).append('=');
            if (cookie.getValue() != null) {
                header.append(cookie.getValue());
            }
            appendCookieAttribute(header, "Path", cookie.getPath());
            appendCookieAttribute(header, "Domain", cookie.getDomain());
            if (cookie.getMaxAge() >= 0) {
                appendCookieAttribute(header, "Max-Age", String.valueOf(cookie.getMaxAge()));
            }
            if (cookie.getSecure()) {
                header.append("; Secure");
            }
            if (cookie.isHttpOnly()) {
                header.append("; HttpOnly");
            }
            appendCookieAttribute(header, "SameSite", sameSitePolicy);
            return header.toString();
        }

        private void appendCookieAttribute(StringBuilder header, String name, String value) {
            if (value != null && !value.isBlank()) {
                header.append("; ").append(name).append('=').append(value);
            }
        }
    }
}
