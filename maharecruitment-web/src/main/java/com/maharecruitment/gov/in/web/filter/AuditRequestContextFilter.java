package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maharecruitment.gov.in.common.audit.AuditRequestContext;
import com.maharecruitment.gov.in.common.audit.ClientIpAddress;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class AuditRequestContextFilter extends OncePerRequestFilter {
    private final TransportSecurityProperties properties;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() { return false; }

    @Override
    protected boolean shouldNotFilterErrorDispatch() { return false; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try (var context = AuditRequestContext.open(ClientIpAddress.resolve(request, properties.isTrustForwardedHeaders()))) {
            chain.doFilter(request, response);
        }
    }
}
