package com.maharecruitment.gov.in.web.security.host;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HostHeaderValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HostHeaderValidationFilter.class);
    private static final int MAX_LOG_VALUE_LENGTH = 300;

    private final HostValidator hostValidator;

    public HostHeaderValidationFilter(HostValidator hostValidator) {
        this.hostValidator = hostValidator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        List<String> hostHeaders = Collections.list(request.getHeaders(HttpHeaders.HOST));
        HostValidationResult validationResult = hostValidator.validate(hostHeaders);
        if (validationResult.valid()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn(
                "Rejected request with invalid Host header. timestamp={} remoteIp={} hostHeader={} requestUri={} reason={}",
                Instant.now(),
                sanitizeForLog(request.getRemoteAddr()),
                sanitizeForLog(String.join(",", hostHeaders)),
                sanitizeForLog(request.getRequestURI()),
                validationResult.reason());

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getWriter().write("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>400 Bad Request</title>
                </head>
                <body>
                    <h1>400 Bad Request</h1>
                    <p>Invalid Host Header</p>
                </body>
                </html>
                """);
    }

    private String sanitizeForLog(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }

        String sanitized = value.replace('\r', '_').replace('\n', '_').replace('\t', '_');
        if (sanitized.length() > MAX_LOG_VALUE_LENGTH) {
            return sanitized.substring(0, MAX_LOG_VALUE_LENGTH) + "...";
        }
        return sanitized;
    }
}
