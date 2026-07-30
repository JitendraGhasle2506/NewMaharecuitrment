package com.maharecruitment.gov.in.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import com.maharecruitment.gov.in.common.security.ApplicationCookieService;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ApplicationCookieService applicationCookieService;

    public CustomAccessDeniedHandler(ApplicationCookieService applicationCookieService) {
        this.applicationCookieService = applicationCookieService;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex)
            throws IOException, ServletException {
        Authentication authentication = resolveAuthentication(request);
        if (shouldTreatAsSessionOrCsrfFailure(ex, authentication)) {
            handleSessionOrCsrfFailure(request, response, ex);
            return;
        }

        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"ACCESS_DENIED\"}");
            return;
        }

        String userRole = resolveUserRole(authentication);
        String url = request.getRequestURI();
        response.setStatus(403);
        response.setContentType("text/html;charset=UTF-8");

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>403 - Access Denied</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
                </head>
                <body class="bg-light">
                    <div class="container d-flex justify-content-center align-items-center" style="height: 100vh;">
                        <div class="card shadow p-4" style="max-width:450px;">
                            <div class="card-body text-center">
                                <h1 class="display-4 text-danger">403</h1>
                                <h3 class="mb-3">Access Denied</h3>
                                <p class="mb-2">You do not have permission to access the requested module.</p>
                                <p class="text-muted mb-2">Role: <strong>%s</strong></p>
                                <p class="text-muted mb-4">URL: <strong>%s</strong></p>
                                <a href="%s/login?accessDenied=true" class="btn btn-primary btn-lg px-4">Go to Login</a>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userRole, url, request.getContextPath());

        response.getWriter().write(html);
    }

    private boolean shouldTreatAsSessionOrCsrfFailure(
            AccessDeniedException exception,
            Authentication authentication) {
        return exception instanceof CsrfException
                || authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
    }

    private void handleSessionOrCsrfFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }
        applicationCookieService.expireRequestCookies(request, response);

        String loginRedirect = buildLoginRedirect(request, exception);
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"SESSION_EXPIRED\",\"redirect\":\"" + loginRedirect + "\"}");
            return;
        }

        response.sendRedirect(loginRedirect);
    }

    private String buildLoginRedirect(HttpServletRequest request, AccessDeniedException exception) {
        if (exception instanceof CsrfException) {
            return request.getContextPath() + "/login?csrfExpired=true";
        }

        if (request.getRequestedSessionId() != null) {
            return request.getContextPath() + "/login?sessionExpired=true";
        }

        return request.getContextPath() + "/login?unauthenticated=true";
    }

    private Authentication resolveAuthentication(HttpServletRequest request) {
        return request.getUserPrincipal() instanceof Authentication auth ? auth : null;
    }

    private String resolveUserRole(Authentication authentication) {
        if (authentication == null) {
            return "ANONYMOUS";
        }

        return authentication.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .reduce((a, b) -> a + ", " + b)
                .orElse("NO_ROLE");
    }
}
