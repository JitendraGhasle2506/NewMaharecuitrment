package com.maharecruitment.gov.in.common.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApplicationCookieService {

    private final CookieSecurityProperties cookieSecurityProperties;
    private final String sameSitePolicy;

    public ApplicationCookieService(CookieSecurityProperties cookieSecurityProperties) {
        this.cookieSecurityProperties = cookieSecurityProperties;
        this.sameSitePolicy = normalizeSameSite(cookieSecurityProperties.getSameSite());

        if ("None".equals(this.sameSitePolicy) && !cookieSecurityProperties.isSecure()) {
            throw new IllegalStateException(
                    "app.security.cookie.secure must be true when app.security.cookie.same-site is None.");
        }
    }

    public Cookie createManagedCookie(String name, String value, String path, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(normalizePath(path));
        cookie.setMaxAge(maxAge);
        cookie.setSecure(cookieSecurityProperties.isSecure());
        cookie.setHttpOnly(cookieSecurityProperties.isHttpOnly());
        cookie.setAttribute("SameSite", sameSitePolicy);
        return cookie;
    }

    public Cookie createExpiredCookie(String name, String path) {
        return createManagedCookie(name, "", path, 0);
    }

    public void expireManagedCookie(HttpServletRequest request, HttpServletResponse response, String cookieName) {
        for (String path : resolveManagedPaths(request)) {
            response.addCookie(createExpiredCookie(cookieName, path));
        }
    }

    public void expireRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        if (request == null || response == null || request.getCookies() == null) {
            return;
        }

        Set<String> cookieNames = new LinkedHashSet<>();
        for (Cookie cookie : request.getCookies()) {
            if (cookie != null && StringUtils.hasText(cookie.getName())) {
                cookieNames.add(cookie.getName());
            }
        }

        cookieNames.forEach(cookieName -> expireManagedCookie(request, response, cookieName));
    }

    private List<String> resolveManagedPaths(HttpServletRequest request) {
        String scopedPath = normalizePath(request != null ? request.getContextPath() : null);
        if ("/".equals(scopedPath)) {
            return List.of("/");
        }
        return List.of(scopedPath, "/");
    }

    private String normalizePath(String path) {
        return StringUtils.hasText(path) ? path.trim() : "/";
    }

    private String normalizeSameSite(String sameSite) {
        if (!StringUtils.hasText(sameSite)) {
            return "Lax";
        }

        return switch (sameSite.trim().toUpperCase()) {
            case "STRICT" -> "Strict";
            case "LAX" -> "Lax";
            case "NONE" -> "None";
            default -> throw new IllegalStateException(
                    "app.security.cookie.same-site must be Strict, Lax, or None.");
        };
    }
}
