package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.maharecruitment.gov.in.web.security.headers.SecurityHeaderPolicy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Applies the browser security policy before any component can complete a
 * response. This covers MVC, REST, static, redirect, error-dispatch and
 * pre-Spring-Security responses consistently.
 */
public class SecurityResponseHeadersFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        SecurityHeadersResponseWrapper wrappedResponse =
                new SecurityHeadersResponseWrapper(request, response);
        wrappedResponse.applySecurityHeaders();
        filterChain.doFilter(request, wrappedResponse);
        if (!wrappedResponse.isCommitted()) {
            wrappedResponse.applySecurityHeaders();
        }
    }

    private static final class SecurityHeadersResponseWrapper extends HttpServletResponseWrapper {

        private final HttpServletRequest request;

        private SecurityHeadersResponseWrapper(
                HttpServletRequest request,
                HttpServletResponse response) {
            super(response);
            this.request = request;
        }

        @Override
        public void reset() {
            super.reset();
            applySecurityHeaders();
        }

        @Override
        public void sendError(int statusCode) throws IOException {
            applySecurityHeaders();
            super.sendError(statusCode);
        }

        @Override
        public void sendError(int statusCode, String message) throws IOException {
            applySecurityHeaders();
            super.sendError(statusCode, message);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            applySecurityHeaders();
            super.sendRedirect(location);
        }

        private void applySecurityHeaders() {
            SecurityHeaderPolicy.writeEarlyResponseHeaders(request, this);
        }
    }
}
