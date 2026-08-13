package com.maharecruitment.gov.in.web.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.maharecruitment.gov.in.auth.constant.CommonConstant;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;

class PasswordChangeRequiredInterceptorTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordChangeRequiredInterceptor interceptor =
            new PasswordChangeRequiredInterceptor(userRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requiredPasswordChangeRedirectsDashboardRequest() throws Exception {
        authenticate();
        MockHttpServletRequest request = request("GET", "/department/home");
        request.getSession().setAttribute(CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo(CommonConstant.PASSWORD_CHANGE_REQUIRED_URL);
        verifyNoInteractions(userRepository);
    }

    @Test
    void requiredPasswordChangeAllowsProfileAndPasswordSubmission() throws Exception {
        authenticate();
        MockHttpServletRequest profileRequest = request("GET", "/common/profile");
        profileRequest.getSession().setAttribute(CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE, true);
        MockHttpServletRequest passwordRequest = request("POST", "/common/profile/password");
        passwordRequest.setSession(profileRequest.getSession());

        assertThat(interceptor.preHandle(profileRequest, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(passwordRequest, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void persistedRequirementIsLoadedForSessionCreatedBeforeDeployment() throws Exception {
        authenticate();
        User user = new User();
        user.setPasswordChangeRequired(true);
        when(userRepository.findByEmailIgnoreCase("first.login@example.com")).thenReturn(Optional.of(user));
        MockHttpServletRequest request = request("GET", "/employee/dashboard");
        request.getSession();

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isFalse();
        assertThat(request.getSession().getAttribute(
                CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE)).isEqualTo(true);
    }

    @Test
    void nullLegacyFlagFailsClosedAndRequiresPasswordChange() throws Exception {
        authenticate();
        User user = new User();
        user.setPasswordChangeRequired(null);
        when(userRepository.findByEmailIgnoreCase("first.login@example.com")).thenReturn(Optional.of(user));
        MockHttpServletRequest request = request("GET", "/employee/dashboard");
        request.getSession();

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isFalse();
        assertThat(request.getSession().getAttribute(
                CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE)).isEqualTo(true);
    }

    @Test
    void completedPasswordChangeAllowsNormalNavigation() throws Exception {
        authenticate();
        MockHttpServletRequest request = request("GET", "/employee/dashboard");
        request.getSession().setAttribute(CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE, false);

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void ajaxRequestReceivesMachineReadableBlockedResponse() throws Exception {
        authenticate();
        MockHttpServletRequest request = request("GET", "/department/data");
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request.getSession().setAttribute(CommonConstant.PASSWORD_CHANGE_REQUIRED_SESSION_ATTRIBUTE, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("PASSWORD_CHANGE_REQUIRED");
    }

    private void authenticate() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("first.login@example.com", "password", "ROLE_EMPLOYEE");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
