package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maharecruitment.gov.in.web.service.mobile.MobileTokenClaims;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenService;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenValidationException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MobileBearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String MOBILE_API_PREFIX = "/api/mobile/";
    private static final String MOBILE_LOGIN_PATH = "/api/mobile/auth/login";
    private static final String MOBILE_REFRESH_PATH = "/api/mobile/auth/refresh";
    private static final String MOBILE_LOGOUT_PATH = "/api/mobile/auth/logout";
    private static final String BEARER_PREFIX = "Bearer ";

    private final MobileTokenService tokenService;
    private final UserDetailsService userDetailsService;

    public MobileBearerTokenAuthenticationFilter(
            MobileTokenService tokenService,
            UserDetailsService userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return !servletPath.startsWith(MOBILE_API_PREFIX)
                || MOBILE_LOGIN_PATH.equals(servletPath)
                || MOBILE_REFRESH_PATH.equals(servletPath)
                || MOBILE_LOGOUT_PATH.equals(servletPath);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String bearerToken = resolveBearerToken(request);
        if (!StringUtils.hasText(bearerToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            MobileTokenClaims claims = tokenService.validateToken(bearerToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.subject());
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    userDetails,
                    null,
                    userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException | MobileTokenValidationException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response);
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0,
                BEARER_PREFIX.length())) {
            return "";
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"INVALID_TOKEN\",\"message\":\"Invalid or expired token.\"}");
    }
}
