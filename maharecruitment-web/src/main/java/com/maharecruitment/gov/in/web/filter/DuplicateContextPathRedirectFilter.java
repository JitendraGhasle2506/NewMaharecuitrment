package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maharecruitment.gov.in.web.security.headers.SecurityHeaderPolicy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DuplicateContextPathRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String normalizedPath = normalizeDuplicateContextPath(request);
        if (normalizedPath == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityHeaderPolicy.writeEarlyResponseHeaders(request, response);
        response.sendRedirect(response.encodeRedirectURL(withQueryString(normalizedPath, request.getQueryString())));
    }

    private String normalizeDuplicateContextPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();

        if (!StringUtils.hasText(contextPath)
                || !StringUtils.hasText(requestUri)
                || !requestUri.startsWith(contextPath)) {
            return null;
        }

        String pathWithinContext = requestUri.substring(contextPath.length());
        String normalizedPathWithinContext = stripDuplicateContextPrefix(pathWithinContext, contextPath);

        if (pathWithinContext.equals(normalizedPathWithinContext)) {
            return null;
        }

        if (!StringUtils.hasText(normalizedPathWithinContext)) {
            normalizedPathWithinContext = "/";
        }

        return contextPath + normalizedPathWithinContext;
    }

    private String stripDuplicateContextPrefix(String path, String contextPath) {
        String normalizedPath = path;
        while (normalizedPath.equals(contextPath) || normalizedPath.startsWith(contextPath + "/")) {
            normalizedPath = normalizedPath.substring(contextPath.length());
        }
        return normalizedPath;
    }

    private String withQueryString(String path, String queryString) {
        if (!StringUtils.hasText(queryString)) {
            return path;
        }
        return path + "?" + queryString;
    }
}
