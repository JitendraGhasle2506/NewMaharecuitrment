package com.maharecruitment.gov.in.web.config;

import java.util.List;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileDTO;
import com.maharecruitment.gov.in.web.service.employee.EmployeeProfileService;
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
    private final EmployeeProfileService employeeProfileService;

    public NavigationModelAdvice(
            NavigationService navigationService,
            ContextPathUrlResolver contextPathUrlResolver,
            EmployeeProfileService employeeProfileService) {
        this.navigationService = navigationService;
        this.contextPathUrlResolver = contextPathUrlResolver;
        this.employeeProfileService = employeeProfileService;
    }

    @ModelAttribute
    public void addNavigationModelAttributes(Model model, HttpServletRequest request) {
        String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
        HttpSession session = request.getSession(false);
        SessionUserDTO sessionUser = extractSessionUser(session);
        List<String> roles = sessionUser != null && sessionUser.roles() != null
                ? sessionUser.roles()
                : List.of();
        sessionUser = refreshEmployeePhotoSessionUser(session, sessionUser, roles);

        String homeUrl = contextPathUrlResolver.resolve(contextPath, resolveHomeUrl(session, roles), "/home");
        List<String> resolvedRoles = List.copyOf(roles);

        model.addAttribute("sessionUser", sessionUser);
        model.addAttribute("loggedInUser", sessionUser);
        model.addAttribute("sessionLoginTime", sessionUser != null ? sessionUser.loginTime() : null);
        model.addAttribute("homePageUrl", homeUrl);
        model.addAttribute("contextPath", contextPath);
        model.addAttribute("primaryRoleLabel", navigationService.resolvePrimaryRoleLabel(resolvedRoles));
    }

    private SessionUserDTO refreshEmployeePhotoSessionUser(HttpSession session, SessionUserDTO sessionUser, List<String> roles) {
        if (session == null || sessionUser == null || roles == null || !roles.contains("ROLE_EMPLOYEE")) {
            return sessionUser;
        }

        try {
            EmployeeProfileDTO profile = employeeProfileService.getCurrentEmployeeProfile(sessionUser.email());
            return storeSessionUser(session, sessionUser, profile);
        } catch (RuntimeException ex) {
            return sessionUser;
        }
    }

    private SessionUserDTO storeSessionUser(HttpSession session, SessionUserDTO sessionUser, EmployeeProfileDTO profile) {
        SessionUserDTO refreshedUser = new SessionUserDTO(
                sessionUser.id(),
                profile.getFullName(),
                profile.getEmail(),
                sessionUser.roles(),
                sessionUser.departmentId(),
                profile.getMobileNo(),
                profile.getPhotoUrl(),
                sessionUser.loginTime(),
                sessionUser.lastLoginTime());
        session.setAttribute(SESSION_USER_KEY, refreshedUser);
        return refreshedUser;
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
