package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class CredentialTransportSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CredentialTransportSecurityFilter.class);

    private static final Set<String> SENSITIVE_PARAMETER_NAMES = Set.of(
            "password",
            "currentpassword",
            "newpassword",
            "confirmpassword");

    private static final Set<String> PROTECTED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final List<String> SENSITIVE_ENDPOINT_PATTERNS = List.of(
            "/login",
            "/doLogin",
            "/authenticate",
            "/login/otp",
            "/login/otp/send",
            "/change-password",
            "/forgot-password",
            "/reset-password",
            "/common/profile/password",
            "/register/**",
            "/admin/user/**",
            "/admin/users",
            "/admin/users/**",
            "/api/verifications/otp/**");

    private static final String HTTPS_REQUIRED_MESSAGE = "HTTPS is required for credential submission.";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final TransportSecurityProperties transportSecurityProperties;

    public CredentialTransportSecurityFilter(TransportSecurityProperties transportSecurityProperties) {
        this.transportSecurityProperties = transportSecurityProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!transportSecurityProperties.isHttpsRequiredFor(request)
                || !isCredentialSubmission(request)
                || isSecureTransport(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = normalizeRequestPath(request);
        log.warn("Rejected credential submission over insecure transport. method={} path={}",
                request.getMethod(), requestPath);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(HTTPS_REQUIRED_MESSAGE);
    }

    private boolean isCredentialSubmission(HttpServletRequest request) {
        if (!PROTECTED_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            return false;
        }

        String requestPath = normalizeRequestPath(request);
        if (SENSITIVE_ENDPOINT_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath))) {
            return true;
        }

        return containsSensitiveParameter(request.getParameterMap());
    }

    private boolean containsSensitiveParameter(Map<String, String[]> parameterMap) {
        return parameterMap.keySet().stream()
                .filter(StringUtils::hasText)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(SENSITIVE_PARAMETER_NAMES::contains);
    }

    private boolean isSecureTransport(HttpServletRequest request) {
        return request.isSecure()
                || (transportSecurityProperties.isTrustForwardedHeaders()
                        && (isForwardedHttps(request.getHeader("X-Forwarded-Proto"))
                                || isForwardedHttps(request.getHeader("Forwarded"))));
    }

    private boolean isForwardedHttps(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return false;
        }

        String normalizedHeader = headerValue.toLowerCase(Locale.ROOT);
        return normalizedHeader.startsWith("https")
                || normalizedHeader.contains("proto=https");
    }

    private String normalizeRequestPath(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }
        return StringUtils.hasText(requestPath) ? requestPath : "/";
    }
}
