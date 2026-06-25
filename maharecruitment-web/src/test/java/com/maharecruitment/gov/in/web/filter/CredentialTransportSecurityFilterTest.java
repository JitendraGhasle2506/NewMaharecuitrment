package com.maharecruitment.gov.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

@ExtendWith(OutputCaptureExtension.class)
class CredentialTransportSecurityFilterTest {

    private final CredentialTransportSecurityFilter filter = new CredentialTransportSecurityFilter();

    @Test
    void httpLoginRequestWithPasswordIsRejected() throws Exception {
        MockHttpServletRequest request = post("/doLogin");
        request.addParameter("username", "user@example.com");
        request.addParameter("password", "SecretPassword123!");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEqualTo("HTTPS is required for credential submission.");
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void httpsLoginRequestWithPasswordIsAllowed() throws Exception {
        MockHttpServletRequest request = post("/doLogin");
        request.setSecure(true);
        request.addParameter("username", "user@example.com");
        request.addParameter("password", "SecretPassword123!");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chainInvoked).isTrue();
    }

    @Test
    void forwardedHttpsCredentialRequestIsAllowedBehindProxy() throws Exception {
        MockHttpServletRequest request = post("/common/profile/password");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addParameter("currentPassword", "OldSecret123!");
        request.addParameter("newPassword", "NewSecret123!");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chainInvoked).isTrue();
    }

    @Test
    void httpChangePasswordRequestIsRejected() throws Exception {
        MockHttpServletRequest request = post("/common/profile/password");
        request.addParameter("currentPassword", "OldSecret123!");
        request.addParameter("newPassword", "NewSecret123!");
        request.addParameter("confirmPassword", "NewSecret123!");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void httpRequestWithPasswordParameterOnAnyApiIsRejected() throws Exception {
        MockHttpServletRequest request = post("/api/accounts/update");
        request.addParameter("newPassword", "NewSecret123!");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, noOpChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void rejectedCredentialSubmissionDoesNotLogPasswordValue(CapturedOutput output) throws Exception {
        String secret = "DoNotLogThisPassword123!";
        MockHttpServletRequest request = post("/doLogin");
        request.addParameter("password", secret);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, noOpChain());

        assertThat(output.getAll()).doesNotContain(secret);
    }

    @Test
    void loopbackHttpCredentialSubmissionIsRejected() throws Exception {
        MockHttpServletRequest request = post("/doLogin");
        request.setServerName("localhost");
        request.setRemoteAddr("127.0.0.1");
        request.addParameter("password", "LocalOnlyPassword123!");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chainInvoked).isFalse();
    }

    private MockHttpServletRequest post(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", requestUri);
        request.setSecure(false);
        return request;
    }

    private jakarta.servlet.FilterChain noOpChain() {
        return (servletRequest, servletResponse) -> {
            try {
                servletResponse.flushBuffer();
            } catch (IOException ex) {
                throw new ServletException(ex);
            }
        };
    }

}
