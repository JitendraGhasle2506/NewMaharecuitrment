package com.maharecruitment.gov.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.maharecruitment.gov.in.web.service.security.LoginCaptchaService;

class LoginCaptchaValidationFilterTest {

    private final LoginCaptchaService captchaService = mock(LoginCaptchaService.class);
    private final LoginCaptchaValidationFilter filter = new LoginCaptchaValidationFilter(captchaService);

    @Test
    void validCaptchaAllowsPasswordAuthenticationToContinue() throws Exception {
        MockHttpServletRequest request = loginRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(captchaService.validateAndConsume(request.getSession(), "challenge", "7")).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void invalidCaptchaStopsPasswordAuthentication() throws Exception {
        MockHttpServletRequest request = loginRequest();
        request.setContextPath("/maharecruitment");
        request.setRequestURI("/maharecruitment/doLogin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getRedirectedUrl())
                .isEqualTo("/maharecruitment/login?loginMode=password&captchaError=true");
        verify(captchaService).validateAndConsume(request.getSession(false), "challenge", "7");
    }

    @Test
    void unrelatedRequestIsNotChecked() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(captchaService);
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/doLogin");
        request.setSession(new MockHttpSession());
        request.addParameter("loginCaptchaId", "challenge");
        request.addParameter("loginCaptchaAnswer", "7");
        return request;
    }
}
