package com.maharecruitment.gov.in.common.util;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class CookieUtil {

    private static final String DEFAULT_SAME_SITE = "Lax";

    // Create secure cookie
    public static Cookie createSecureCookie(String name, String value, int maxAge) {
        return createCookie(name, value, "/", maxAge, true);
    }

    public static Cookie createCookie(String name, String value, String path, int maxAge, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath((path == null || path.isBlank()) ? "/" : path);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(secure);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", DEFAULT_SAME_SITE);
        return cookie;
    }

    // Delete cookie securely
    public static Cookie deleteSecureCookie(String name) {
        return deleteCookie(name, "/", true);
    }

    public static Cookie deleteCookie(String name, String path, boolean secure) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath((path == null || path.isBlank()) ? "/" : path);
        cookie.setMaxAge(0);
        cookie.setSecure(secure);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", DEFAULT_SAME_SITE);
        return cookie;
    }

    public static boolean isSecureRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        if (request.isSecure()) {
            return true;
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && forwardedProto.toLowerCase().contains("https")) {
            return true;
        }

        String forwarded = request.getHeader("Forwarded");
        return forwarded != null && forwarded.toLowerCase().contains("proto=https");
    }
}
