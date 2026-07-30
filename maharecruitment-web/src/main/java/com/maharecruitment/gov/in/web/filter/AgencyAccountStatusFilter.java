package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maharecruitment.gov.in.auth.service.AgencyAccountAccessService;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.service.agency.AgencyAccessService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AgencyAccountStatusFilter extends OncePerRequestFilter {

    private static final String ROLE_AGENCY = "ROLE_AGENCY";

    private final AgencyAccessService agencyAccessService;

    public AgencyAccountStatusFilter(AgencyAccessService agencyAccessService) {
        this.agencyAccessService = agencyAccessService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = resolvePath(request);
        return !path.equals("/agency") && !path.startsWith("/agency/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !hasAgencyRole(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            agencyAccessService.requireActiveAgencyContext(authentication.getName());
            filterChain.doFilter(request, response);
        } catch (RecruitmentNotificationException ex) {
            writeForbidden(response, ex.getMessage());
        }
    }

    private boolean hasAgencyRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_AGENCY.equals(authority.getAuthority()));
    }

    private String resolvePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        String safeMessage = StringUtils.hasText(message)
                ? message
                : AgencyAccountAccessService.INACTIVE_AGENCY_MESSAGE;
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write(safeMessage);
    }
}
