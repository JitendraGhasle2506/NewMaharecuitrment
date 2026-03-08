package com.maharecruitment.gov.in.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.maharecruitment.gov.in.common.util.CookieUtil;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex)
            throws IOException, ServletException {

        /* -----------------------------------------
           1.  Invalidate Session
        ----------------------------------------- */
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }

        /* -----------------------------------------
           2. Securely Delete All Cookies
        ----------------------------------------- */
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                response.addCookie(CookieUtil.deleteSecureCookie(c.getName()));
            }
        }

        /* -----------------------------------------
           3. AJAX Request? Return JSON
        ----------------------------------------- */
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"ACCESS_DENIED\",\"redirect\":\"" + request.getContextPath() + "/login?accessDenied=true\"}");
            return;
        }

        /* -----------------------------------------
           4. Dynamic Role Detection (any role)
        ----------------------------------------- */
        String userRole = "ANONYMOUS";

        if (request.getUserPrincipal() instanceof Authentication auth) {
            userRole = auth.getAuthorities()
                    .stream()
                    .map(a -> a.getAuthority())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("NO_ROLE");
        }

        /* -----------------------------------------
           5. Requested URL
        ----------------------------------------- */
        String url = request.getRequestURI();

        /* -----------------------------------------
           6. Create Beautiful 403 Page
        ----------------------------------------- */
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
                                <p class="mb-2">Your session was terminated for security reasons.</p>
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
}
