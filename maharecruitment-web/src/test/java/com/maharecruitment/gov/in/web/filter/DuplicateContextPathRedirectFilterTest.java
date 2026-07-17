package com.maharecruitment.gov.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.maharecruitment.gov.in.web.security.headers.SecurityHeaderPolicy;

import jakarta.servlet.ServletException;

class DuplicateContextPathRedirectFilterTest {

    private static final String CONTEXT_PATH = "/maharecruitment";

    private final DuplicateContextPathRedirectFilter filter = new DuplicateContextPathRedirectFilter();

    @Test
    void duplicateContextPathIsRedirectedToSingleContextPath() throws Exception {
        MockHttpServletRequest request = get(CONTEXT_PATH + CONTEXT_PATH + CONTEXT_PATH + "/login");
        request.setQueryString("unauthenticated=true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo(CONTEXT_PATH + "/login?unauthenticated=true");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Content-Security-Policy"))
                .isEqualTo(SecurityHeaderPolicy.CONTENT_SECURITY_POLICY);
        assertThat(response.getHeader("Strict-Transport-Security")).isNull();
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void duplicateContextRootIsRedirectedToApplicationRoot() throws Exception {
        MockHttpServletRequest request = get(CONTEXT_PATH + CONTEXT_PATH + CONTEXT_PATH + "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, noOpChain());

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo(CONTEXT_PATH + "/");
    }

    @Test
    void doubleContextRootIsRedirectedToApplicationRoot() throws Exception {
        MockHttpServletRequest request = get(CONTEXT_PATH + CONTEXT_PATH + "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, noOpChain());

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo(CONTEXT_PATH + "/");
    }

    @Test
    void normalContextPathRequestPassesThrough() throws Exception {
        MockHttpServletRequest request = get(CONTEXT_PATH + "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getRedirectedUrl()).isNull();
        assertThat(chainInvoked).isTrue();
    }

    private MockHttpServletRequest get(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setContextPath(CONTEXT_PATH);
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
