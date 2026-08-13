package com.maharecruitment.gov.in.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.maharecruitment.gov.in.auth.constant.CommonConstant;
import com.maharecruitment.gov.in.auth.dto.UserAffiliationView;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.auth.service.UserLoginTrackingService;

class MySimpleUrlAuthenticationSuccessHandlerTest {

    private final UserAffiliationService affiliationService = mock(UserAffiliationService.class);
    private final UserLoginTrackingService loginTrackingService = mock(UserLoginTrackingService.class);
    private final MySimpleUrlAuthenticationSuccessHandler handler =
            new MySimpleUrlAuthenticationSuccessHandler(affiliationService, loginTrackingService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void temporaryPasswordRedirectsToMandatoryPasswordChange() throws Exception {
        User user = user(true);
        stubAffiliation(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication());

        assertThat(response.getRedirectedUrl()).isEqualTo(CommonConstant.PASSWORD_CHANGE_REQUIRED_URL);
        assertThat(request.getSession().getAttribute(
                CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE)).isEqualTo(true);
        assertThat(request.getSession().getAttribute("homepageUrl")).isEqualTo("/employee/dashboard");
    }

    @Test
    void completedPasswordChangeRedirectsToRoleDashboard() throws Exception {
        User user = user(false);
        user.setLastLoginAt(LocalDateTime.now().minusDays(1));
        stubAffiliation(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication());

        assertThat(response.getRedirectedUrl()).isEqualTo("/employee/dashboard");
    }

    @Test
    void firstLoginForcesPasswordChangeEvenWhenLegacyFlagIsFalse() throws Exception {
        User user = user(false);
        stubAffiliation(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication());

        assertThat(response.getRedirectedUrl()).isEqualTo(CommonConstant.PASSWORD_CHANGE_REQUIRED_URL);
        assertThat(user.getPasswordChangeRequired()).isTrue();
        assertThat(request.getSession().getAttribute(
                CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE)).isEqualTo(true);
    }

    private void stubAffiliation(User user) {
        UserAffiliationView affiliation = UserAffiliationView.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roleNames(List.of("ROLE_EMPLOYEE"))
                .build();
        when(affiliationService.loadUserByEmail(user.getEmail())).thenReturn(user);
        when(affiliationService.getAffiliation(user)).thenReturn(affiliation);
        when(loginTrackingService.recordSuccessfulLogin(eq(user), any())).thenReturn(user.getLastLoginAt());
    }

    private User user(boolean passwordChangeRequired) {
        User user = new User();
        user.setId(41L);
        user.setName("First Login User");
        user.setEmail("first.login@example.com");
        user.setPasswordChangeRequired(passwordChangeRequired);
        user.setRoles(List.of(new Role(null, "ROLE_EMPLOYEE", List.of(), List.of())));
        return user;
    }

    private TestingAuthenticationToken authentication() {
        return new TestingAuthenticationToken(
                "first.login@example.com", "password", "ROLE_EMPLOYEE");
    }
}
