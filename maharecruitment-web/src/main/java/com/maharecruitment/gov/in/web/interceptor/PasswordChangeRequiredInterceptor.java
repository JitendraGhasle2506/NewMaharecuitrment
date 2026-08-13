package com.maharecruitment.gov.in.web.interceptor;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.maharecruitment.gov.in.auth.constant.CommonConstant;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {

    private static final String PROFILE_PATH = "/common/profile";
    private static final String PASSWORD_PATH = "/common/profile/password";
    private static final String LOGOUT_PATH = "/logout";
    private static final String CREDENTIAL_KEY_PATH = "/security/credential-encryption/public-key";

    private final UserRepository userRepository;

    public PasswordChangeRequiredInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || !isPasswordChangeRequired(session, authentication.getName())) {
            return true;
        }

        String path = applicationPath(request);
        if (isPasswordChangeRequest(path, request.getMethod())
                || LOGOUT_PATH.equals(path)
                || CREDENTIAL_KEY_PATH.equals(path)) {
            return true;
        }

        String redirectUrl = request.getContextPath() + CommonConstant.PASSWORD_CHANGE_REQUIRED_URL;
        if (isAjaxRequest(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"error\":\"PASSWORD_CHANGE_REQUIRED\",\"redirect\":\""
                            + redirectUrl
                            + "\"}");
            return false;
        }

        response.sendRedirect(redirectUrl);
        return false;
    }

    private boolean isPasswordChangeRequired(HttpSession session, String email) {
        Object sessionValue = session.getAttribute(CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE);
        if (sessionValue instanceof Boolean required) {
            return required;
        }

        boolean required = Optional.ofNullable(email)
                .flatMap(userRepository::findByEmailIgnoreCase)
                .map(user -> !Boolean.FALSE.equals(user.getPasswordChangeRequired()))
                .orElse(false);
        session.setAttribute(CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE, required);
        return required;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isPasswordChangeRequest(String path, String method) {
        return (PROFILE_PATH.equals(path) && "GET".equalsIgnoreCase(method))
                || (PASSWORD_PATH.equals(path) && "POST".equalsIgnoreCase(method));
    }

    private String applicationPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath.isEmpty() ? requestUri : requestUri.substring(contextPath.length());
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || Optional.ofNullable(request.getHeader("Accept"))
                        .map(value -> value.contains(MediaType.APPLICATION_JSON_VALUE))
                        .orElse(false);
    }
}
