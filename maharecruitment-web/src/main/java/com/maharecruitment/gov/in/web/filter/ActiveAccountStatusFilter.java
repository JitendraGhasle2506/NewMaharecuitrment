package com.maharecruitment.gov.in.web.filter;

import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Recheck persisted status so deactivation also blocks previously issued sessions and bearer tokens. */
public class ActiveAccountStatusFilter extends OncePerRequestFilter {
    private final UserRepository users;

    public ActiveAccountStatusFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || users.existsByEmailIgnoreCaseAndActiveTrue(authentication.getName())) {
            chain.doFilter(request, response);
            return;
        }
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String accept = request.getHeader("Accept");
        if (path.startsWith("/api/") || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"ACCOUNT_DISABLED\",\"message\":\"Your account is inactive. Contact HR.\"}");
        } else {
            response.sendRedirect(request.getContextPath() + "/login?disabled");
        }
    }
}
