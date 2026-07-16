package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Rejects methods outside the application's documented HTTP method policy. */
public final class HttpMethodPolicyFilter extends OncePerRequestFilter {

    private static final Set<String> APPLICATION_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "DELETE", "PATCH");

    private final boolean optionsEnabled;

    public HttpMethodPolicyFilter(@Value("${app.security.http-methods.allow-options:false}") boolean optionsEnabled) {
        this.optionsEnabled = optionsEnabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        if (APPLICATION_METHODS.contains(method) || (optionsEnabled && "OPTIONS".equals(method))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":false,\"message\":\"HTTP method "
                + jsonEscape(method) + " is not allowed.\"}");
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
