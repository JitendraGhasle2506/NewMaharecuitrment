package com.maharecruitment.gov.in.web.security.headers;

import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Central security-header values shared by Spring Security and responses that
 * intentionally terminate before Spring Security's HeaderWriterFilter.
 */
public final class SecurityHeaderPolicy {

    public static final String CSP_NONCE_REQUEST_ATTRIBUTE =
            SecurityHeaderPolicy.class.getName() + ".nonce";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CSP_NONCE_BYTES = 18;

    public static final String PERMISSIONS_POLICY = String.join(", ",
            "camera=()",
            "microphone=()",
            "geolocation=()",
            "payment=()",
            "usb=()",
            "accelerometer=()",
            "gyroscope=()",
            "magnetometer=()");

    private static final String INVOICE_APPLICATION_PREFIX = "/invoice/tax-invoices/application/";

    private SecurityHeaderPolicy() {
    }

    /**
     * Invoice preview pages are intentionally rendered inside same-origin iframes.
     * All other responses retain DENY/frame-ancestors 'none'.
     */
    public static boolean allowsSameOriginFraming(HttpServletRequest request) {
        String path = applicationPath(request);
        if (!path.startsWith(INVOICE_APPLICATION_PREFIX)) {
            return false;
        }

        String applicationPath = path.substring(INVOICE_APPLICATION_PREFIX.length());
        int firstSlash = applicationPath.indexOf('/');
        if (firstSlash < 1 || firstSlash == applicationPath.length() - 1) {
            return false;
        }

        String viewPath = applicationPath.substring(firstSlash + 1);
        return viewPath.equals("new")
                || viewPath.equals("preview")
                || viewPath.startsWith("preview/");
    }

    /**
     * Applies headers to responses produced before HeaderWriterFilter can run.
     */
    public static void writeEarlyResponseHeaders(
            HttpServletRequest request,
            HttpServletResponse response) {
        boolean sameOriginFraming = allowsSameOriginFraming(request);
        setIfAbsent(response, "X-Content-Type-Options", "nosniff");
        setIfAbsent(response, "X-Frame-Options", sameOriginFraming ? "SAMEORIGIN" : "DENY");
        setIfAbsent(response, "Referrer-Policy", "strict-origin-when-cross-origin");
        setIfAbsent(response, "Permissions-Policy", PERMISSIONS_POLICY);
        setIfAbsent(
                response,
                "Content-Security-Policy",
                contentSecurityPolicy(request));
        setIfAbsent(response, "X-XSS-Protection", "0");
        if (request.isSecure()) {
            setIfAbsent(
                    response,
                    "Strict-Transport-Security",
                    "max-age=31536000 ; includeSubDomains");
        }
    }

    public static String nonce(HttpServletRequest request) {
        Object existing = request.getAttribute(CSP_NONCE_REQUEST_ATTRIBUTE);
        if (existing instanceof String value && !value.isBlank()) {
            return value;
        }
        byte[] nonceBytes = new byte[CSP_NONCE_BYTES];
        SECURE_RANDOM.nextBytes(nonceBytes);
        String nonce = Base64.getEncoder().withoutPadding().encodeToString(nonceBytes);
        request.setAttribute(CSP_NONCE_REQUEST_ATTRIBUTE, nonce);
        return nonce;
    }

    public static void writeContentSecurityPolicy(
            HttpServletRequest request,
            HttpServletResponse response) {
        response.setHeader(
                "Content-Security-Policy",
                contentSecurityPolicy(request));
    }

    public static void writeFinalContentSecurityPolicy(
            HttpServletRequest request,
            HttpServletResponse response) {
        writeContentSecurityPolicy(request, response);
    }

    public static String contentSecurityPolicy(HttpServletRequest request) {
        String requestNonce = nonce(request);
        return String.join(" ",
                "default-src 'self';",
                "script-src 'self' 'nonce-" + requestNonce
                        + "' https://code.jquery.com https://cdn.jsdelivr.net https://unpkg.com;",
                "script-src-attr 'none';",
                "style-src 'self' 'nonce-" + requestNonce
                        + "' https://cdn.jsdelivr.net https://unpkg.com;",
                "style-src-attr 'none';",
                "img-src 'self' data: blob: https://*.tile.openstreetmap.org https://cdn.jsdelivr.net https://unpkg.com;",
                "font-src 'self' data: https://cdn.jsdelivr.net https://unpkg.com;",
                "connect-src 'self' https://nominatim.openstreetmap.org;",
                "object-src 'none';",
                "frame-src 'self';",
                "base-uri 'self';",
                "form-action 'self';",
                allowsSameOriginFraming(request) ? "frame-ancestors 'self';" : "frame-ancestors 'none';");
    }

    private static void setIfAbsent(HttpServletResponse response, String name, String value) {
        if (!response.containsHeader(name)) {
            response.setHeader(name, value);
        }
    }

    private static String applicationPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
