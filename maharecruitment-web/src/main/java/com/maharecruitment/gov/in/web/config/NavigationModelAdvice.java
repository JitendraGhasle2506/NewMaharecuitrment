package com.maharecruitment.gov.in.web.config;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.web.service.navigation.NavigationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class NavigationModelAdvice {

    private static final String SESSION_USER_KEY = "SESSION_USER";
    private static final String HOMEPAGE_URL_KEY = "homepageUrl";

    private final NavigationService navigationService;

    public NavigationModelAdvice(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @ModelAttribute
    public void addNavigationModelAttributes(Model model, HttpServletRequest request) {
        String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
        HttpSession session = request.getSession(false);
        SessionUserDTO sessionUser = extractSessionUser(session);
        List<String> roles = sessionUser != null && sessionUser.roles() != null
                ? sessionUser.roles()
                : List.of();

        String homeUrl = toContextAwareUrl(contextPath, resolveHomeUrl(session, roles));
        List<String> resolvedRoles = List.copyOf(roles);

        model.addAttribute("homePageUrl", homeUrl);
        model.addAttribute("contextPath", contextPath);
        model.addAttribute("sidebarItems", navigationService.resolveSidebarItems(resolvedRoles)
                .stream()
                .map(item -> item.withUrl(toContextAwareUrl(contextPath, item.url())))
                .collect(Collectors.toList()));
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

    private String toContextAwareUrl(String contextPath, String url) {
        if (url == null || url.isBlank()) {
            return contextPath + "/home";
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        if (contextPath.isBlank()) {
            return url.startsWith("/") ? url : "/" + url;
        }

        if (url.startsWith(contextPath)) {
            return url;
        }

        return url.startsWith("/") ? contextPath + url : contextPath + "/" + url;
    }
}
