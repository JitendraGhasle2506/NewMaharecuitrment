package com.maharecruitment.gov.in.web.config;

import java.util.List;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.web.service.navigation.NavigationService;
import com.maharecruitment.gov.in.web.util.ContextPathUrlResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class NavigationModelAdvice {

    private static final String SESSION_USER_KEY = "SESSION_USER";
    private static final String HOMEPAGE_URL_KEY = "homepageUrl";

    private final NavigationService navigationService;
    private final ContextPathUrlResolver contextPathUrlResolver;

    public NavigationModelAdvice(
            NavigationService navigationService,
            ContextPathUrlResolver contextPathUrlResolver) {
        this.navigationService = navigationService;
        this.contextPathUrlResolver = contextPathUrlResolver;
    }

    @ModelAttribute
    public void addNavigationModelAttributes(Model model, HttpServletRequest request) {
        String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
        HttpSession session = request.getSession(false);
        SessionUserDTO sessionUser = extractSessionUser(session);
        List<String> roles = sessionUser != null && sessionUser.roles() != null
                ? sessionUser.roles()
                : List.of();

        String homeUrl = contextPathUrlResolver.resolve(contextPath, resolveHomeUrl(session, roles), "/home");
        List<String> resolvedRoles = List.copyOf(roles);

        model.addAttribute("sessionUser", sessionUser);
        model.addAttribute("sessionLoginTime", sessionUser != null ? sessionUser.loginTime() : null);
        model.addAttribute("homePageUrl", homeUrl);
        model.addAttribute("contextPath", contextPath);
        model.addAttribute("primaryRoleLabel", navigationService.resolvePrimaryRoleLabel(resolvedRoles));
    }

    private SessionUserDTO extractSessionUser(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object sessionUser = session.getAttribute(SESSION_USER_KEY);
        if (sessionUser instanceof SessionUserDTO dto) {
            return dto;
        }

        return null;
    }

    private String resolveHomeUrl(HttpSession session, List<String> roles) {
        if (session != null) {
            Object homepageUrl = session.getAttribute(HOMEPAGE_URL_KEY);
            if (homepageUrl instanceof String targetUrl && !targetUrl.isBlank()) {
                return targetUrl;
            }
        }

        return navigationService.resolveHomeUrl(roles);
    }

}
