package com.maharecruitment.gov.in.web.security.host;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HostHeaderValidationFilterTest {

    private final HostHeaderValidationFilter filter = new HostHeaderValidationFilter(validator());

    @Test
    void validHostContinuesFilterChain() throws Exception {
        MockHttpServletRequest request = requestWithHost("portal.example.gov.in");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chainInvoked).isTrue();
    }

    @Test
    void invalidHostReturnsBadRequestWithoutFrameworkErrorPage() throws Exception {
        MockHttpServletRequest request = requestWithHost("evil.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("Invalid Host Header");
        assertThat(response.getContentAsString()).doesNotContain("Exception");
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void multipleHostHeadersReturnBadRequest() throws Exception {
        MockHttpServletRequest request = requestWithHost("portal.example.gov.in");
        request.addHeader(HttpHeaders.HOST, "evil.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void missingHostHeaderReturnsBadRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void forwardedHostAttackIsIgnoredWhenHostIsValid() throws Exception {
        MockHttpServletRequest request = requestWithHost("portal.example.gov.in");
        request.addHeader("X-Forwarded-Host", "evil.example.com");
        request.addHeader("Forwarded", "host=evil.example.com;proto=https");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chainInvoked).isTrue();
    }

    @Test
    void forwardedHostCannotRescueInvalidHostHeader() throws Exception {
        MockHttpServletRequest request = requestWithHost("evil.example.com");
        request.addHeader("X-Forwarded-Host", "portal.example.gov.in");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(response.getStatus()).isEqualTo(400);
    }

    private MockHttpServletRequest requestWithHost(String host) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        request.addHeader(HttpHeaders.HOST, host);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }

    private HostValidator validator() {
        HostProperties properties = new HostProperties();
        properties.setAllowedHosts(List.of(
                "portal.example.gov.in",
                "www.portal.example.gov.in",
                "localhost",
                "127.0.0.1"));
        properties.setAllowedPorts(Set.of(80, 443, 8443));
        return new HostValidator(properties);
    }
}
