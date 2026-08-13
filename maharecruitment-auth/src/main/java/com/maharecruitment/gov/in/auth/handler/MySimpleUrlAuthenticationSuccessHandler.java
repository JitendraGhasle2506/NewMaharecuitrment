package com.maharecruitment.gov.in.auth.handler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.auth.constant.CommonConstant;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.auth.dto.UserAffiliationView;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.auth.service.UserLoginTrackingService;
import com.maharecruitment.gov.in.auth.util.AuthorityUtil;
import com.maharecruitment.gov.in.common.security.AuthenticationAuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class MySimpleUrlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(MySimpleUrlAuthenticationSuccessHandler.class);

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final UserAffiliationService userAffiliationService;
    private final UserLoginTrackingService userLoginTrackingService;
    private final AuthenticationAuditService authenticationAuditService;

    public MySimpleUrlAuthenticationSuccessHandler(
            UserAffiliationService userAffiliationService,
            UserLoginTrackingService userLoginTrackingService,
            AuthenticationAuditService authenticationAuditService) {
        this.userAffiliationService = userAffiliationService;
        this.userLoginTrackingService = userLoginTrackingService;
        this.authenticationAuditService = authenticationAuditService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        boolean passwordChangeRequired = storeUserInSession(request, session, authentication);
        String homepageUrl = determineTargetUrl(authentication);
        session.setAttribute("homepageUrl", homepageUrl);
        String targetUrl = passwordChangeRequired
                ? CommonConstant.PASSWORD_CHANGE_REQUIRED_URL
                : homepageUrl;
        handle(request, response, targetUrl);
        clearAuthenticationAttributes(request);

        logger.info("Authentication and SESSION_USER stored successfully.");
    }

    protected void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            String targetUrl) throws IOException {
        if (response.isCommitted()) {
            logger.debug("Response already committed. Cannot redirect to {}", targetUrl);
            return;
        }

        redirectStrategy.sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(Authentication authentication) {
        Map<String, String> roleTargetUrlMap = CommonConstant.getDashboardUrls();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority grantedAuthority : authorities) {
            String authorityName = grantedAuthority.getAuthority();
            if (roleTargetUrlMap.containsKey(authorityName)) {
                return roleTargetUrlMap.get(authorityName);
            }
        }

        return "/home";
    }

    protected final void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
    }

    private boolean storeUserInSession(
            HttpServletRequest request,
            HttpSession session,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userAffiliationService.loadUserByEmail(email);
        UserAffiliationView affiliation = userAffiliationService.getAffiliation(user);
        boolean firstLogin = user.getLastLoginAt() == null;
        boolean passwordChangeRequired = firstLogin
                || !Boolean.FALSE.equals(user.getPasswordChangeRequired());
        if (passwordChangeRequired) {
            // Persist the requirement before last_login_at is updated so it cannot be
            // bypassed by closing the first session without changing the password.
            user.setPasswordChangeRequired(true);
        }
        LocalDateTime loginTime = LocalDateTime.now();
        LocalDateTime lastLoginTime = userLoginTrackingService.recordSuccessfulLogin(user, loginTime);

        List<String> roles = user.getRoles() == null ? List.of() : user.getRoles().stream()
                .map(role -> AuthorityUtil.toAuthority(role.getName()))
                .filter(r -> r != null && !r.isBlank())
                .collect(Collectors.toList());

        SessionUserDTO sessionUser = new SessionUserDTO(
                affiliation.getUserId(),
                affiliation.getName(),
                affiliation.getEmail(),
                roles,
                affiliation.getDepartmentRegistrationId(),
                affiliation.getMobileNo(),
                null,
                loginTime,
                lastLoginTime
        );

        session.setAttribute("SESSION_USER", sessionUser);
        session.setAttribute(
                CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE,
                passwordChangeRequired);
        recordLoginAudit(request, session, user);
        logger.info(
                "Login routing evaluated. userId={}, firstLogin={}, passwordChangeRequired={}",
                user.getId(),
                firstLogin,
                passwordChangeRequired);
        return passwordChangeRequired;
    }

    private void recordLoginAudit(HttpServletRequest request, HttpSession session, User user) {
        try {
            authenticationAuditService.recordLogin(
                    user.getId(),
                    user.getEmail(),
                    session.getId(),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"),
                    resolveAuthenticationMethod(request),
                    AuthenticationAuditService.SOURCE_WEB);
        } catch (RuntimeException ex) {
            logger.error(
                    "Unable to persist successful login audit. userId={}, errorType={}",
                    user.getId(),
                    ex.getClass().getSimpleName(),
                    ex);
        }
    }

    private String resolveAuthenticationMethod(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        return requestPath != null && requestPath.endsWith("/login/otp")
                ? AuthenticationAuditService.METHOD_OTP
                : AuthenticationAuditService.METHOD_PASSWORD;
    }
}
