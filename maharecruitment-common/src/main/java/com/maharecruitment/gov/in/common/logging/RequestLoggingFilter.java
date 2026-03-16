package com.maharecruitment.gov.in.common.logging;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USERNAME_KEY = "username";
    private static final String CLIENT_IP_KEY = "clientIp";
    private static final String HTTP_METHOD_KEY = "httpMethod";
    private static final String REQUEST_PATH_KEY = "requestPath";

    private static final List<String> EXCLUDED_PATTERNS = List.of(
            "/css/**",
            "/js/**",
            "/images/**",
            "/img/**",
            "/icons/**",
            "/webjars/**",
            "/error/**",
            "/favicon.ico");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = normalizeRequestPath(request);
        return EXCLUDED_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String requestId = resolveRequestId(request);
        String requestPath = normalizeRequestPath(request);

        MDC.put(REQUEST_ID_KEY, requestId);
        MDC.put(HTTP_METHOD_KEY, request.getMethod());
        MDC.put(REQUEST_PATH_KEY, requestPath);
        MDC.put(CLIENT_IP_KEY, resolveClientIp(request));
        response.setHeader(REQUEST_ID_HEADER, requestId);

        Throwable error = null;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            error = ex;
            throw ex;
        } finally {
            String username = resolveUsername();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            int status = response.getStatus();

            MDC.put(USERNAME_KEY, username);
            if (error == null) {
                LOGGER.info("HTTP {} {} status={} durationMs={} user={}",
                        request.getMethod(), requestPath, status, durationMs, username);
            } else {
                LOGGER.error("HTTP {} {} failed status={} durationMs={} user={}",
                        request.getMethod(), requestPath, status, durationMs, username, error);
            }

            MDC.remove(REQUEST_ID_KEY);
            MDC.remove(USERNAME_KEY);
            MDC.remove(CLIENT_IP_KEY);
            MDC.remove(HTTP_METHOD_KEY);
            MDC.remove(REQUEST_PATH_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return StringUtils.hasText(requestId) ? requestId.trim() : UUID.randomUUID().toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }

        String username = authentication.getName();
        return StringUtils.hasText(username) ? username : "anonymous";
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
