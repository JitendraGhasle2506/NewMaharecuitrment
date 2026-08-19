package com.maharecruitment.gov.in.web.security.headers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Central security-header values shared by Spring Security and responses that
 * intentionally terminate before Spring Security's HeaderWriterFilter.
 */
public final class SecurityHeaderPolicy {

    public static final String CSP_NONCE_REQUEST_ATTRIBUTE =
            SecurityHeaderPolicy.class.getName() + ".nonce";
    static final String CSP_SCRIPT_ATTRIBUTE_HASHES_REQUEST_ATTRIBUTE =
            SecurityHeaderPolicy.class.getName() + ".scriptAttributeHashes";
    static final String CSP_STYLE_ATTRIBUTE_HASHES_REQUEST_ATTRIBUTE =
            SecurityHeaderPolicy.class.getName() + ".styleAttributeHashes";

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
                contentSecurityPolicy(request, Set.of(), Set.of()));
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
            HttpServletResponse response,
            Collection<String> scriptAttributeHashes,
            Collection<String> styleAttributeHashes) {
        response.setHeader(
                "Content-Security-Policy",
                contentSecurityPolicy(request, scriptAttributeHashes, styleAttributeHashes));
    }

    public static void writeFinalContentSecurityPolicy(
            HttpServletRequest request,
            HttpServletResponse response) {
        writeContentSecurityPolicy(
                request,
                response,
                registeredHashes(request, CSP_SCRIPT_ATTRIBUTE_HASHES_REQUEST_ATTRIBUTE),
                registeredHashes(request, CSP_STYLE_ATTRIBUTE_HASHES_REQUEST_ATTRIBUTE));
    }

    static void registerScriptAttributeHash(HttpServletRequest request, String attributeValue) {
        registerHash(request, CSP_SCRIPT_ATTRIBUTE_HASHES_REQUEST_ATTRIBUTE, attributeValue);
    }

    static void registerStyleAttributeHash(HttpServletRequest request, String attributeValue) {
        registerHash(request, CSP_STYLE_ATTRIBUTE_HASHES_REQUEST_ATTRIBUTE, attributeValue);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", ex);
        }
    }

    public static String contentSecurityPolicy(
            HttpServletRequest request,
            Collection<String> scriptAttributeHashes,
            Collection<String> styleAttributeHashes) {
        String requestNonce = nonce(request);
        return String.join(" ",
                "default-src 'self';",
                "script-src 'self' 'nonce-" + requestNonce
                        + "' https://code.jquery.com https://cdn.jsdelivr.net https://unpkg.com;",
                attributeDirective("script-src-attr", scriptAttributeHashes),
                "style-src 'self' 'nonce-" + requestNonce
                        + "' https://cdn.jsdelivr.net https://unpkg.com;",
                attributeDirective("style-src-attr", styleAttributeHashes),
                "img-src 'self' data: blob: https://*.tile.openstreetmap.org https://cdn.jsdelivr.net https://unpkg.com;",
                "font-src 'self' data: https://cdn.jsdelivr.net https://unpkg.com;",
                "connect-src 'self' https://nominatim.openstreetmap.org;",
                "object-src 'none';",
                "frame-src 'self';",
                "base-uri 'self';",
                "form-action 'self';",
                allowsSameOriginFraming(request) ? "frame-ancestors 'self';" : "frame-ancestors 'none';");
    }

    private static String attributeDirective(String directive, Collection<String> hashes) {
        Set<String> uniqueHashes = hashes == null ? Set.of() : new LinkedHashSet<>(hashes);
        if (uniqueHashes.isEmpty()) {
            return directive + " 'none';";
        }
        StringBuilder value = new StringBuilder(directive).append(" 'unsafe-hashes'");
        uniqueHashes.forEach(hash -> value.append(" 'sha256-").append(hash).append("'"));
        return value.append(';').toString();
    }

    @SuppressWarnings("unchecked")
    private static void registerHash(
            HttpServletRequest request,
            String requestAttribute,
            String attributeValue) {
        Object existing = request.getAttribute(requestAttribute);
        Set<String> hashes;
        if (existing instanceof Set<?>) {
            hashes = (Set<String>) existing;
        } else {
            hashes = new LinkedHashSet<>();
            request.setAttribute(requestAttribute, hashes);
        }
        hashes.add(sha256(attributeValue));
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> registeredHashes(
            HttpServletRequest request,
            String requestAttribute) {
        Object hashes = request.getAttribute(requestAttribute);
        return hashes instanceof Set<?> ? List.copyOf((Set<String>) hashes) : List.of();
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
