package com.maharecruitment.gov.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.maharecruitment.gov.in.common.security.CookieSecurityProperties;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

class CookieAttributeFilterTest {

    @Test
    void setCookieHeaderContainsSecureHttpOnlyAndSameSite() throws Exception {
        CookieAttributeFilter filter = new CookieAttributeFilter(secureCookieProperties(), strictTransport());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (request, servletResponse) ->
                ((HttpServletResponse) servletResponse).addHeader(
                        "Set-Cookie",
                        "JSESSIONID=abc123; Path=/maharecruitment"));

        assertThat(response.getHeaders("Set-Cookie")).hasSize(1);
        assertThat(response.getHeader("Set-Cookie"))
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }

    @Test
    void addedCookieContainsSecureHttpOnlyAndSameSite() throws Exception {
        CookieAttributeFilter filter = new CookieAttributeFilter(secureCookieProperties(), strictTransport());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (request, servletResponse) -> {
            Cookie cookie = new Cookie("JSESSIONID", "abc123");
            cookie.setPath("/maharecruitment");
            ((HttpServletResponse) servletResponse).addCookie(cookie);
        });

        assertThat(response.getHeaders("Set-Cookie")).hasSize(1);
        assertThat(response.getHeader("Set-Cookie"))
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }

    @Test
    void loopbackHttpCookieDoesNotUseSecureFlag() throws Exception {
        CookieAttributeFilter filter = new CookieAttributeFilter(secureCookieProperties(), localTransport());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(false);
        request.setServerName("localhost");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).addHeader(
                        "Set-Cookie",
                        "JSESSIONID=abc123; Path=/maharecruitment; Secure"));

        assertThat(response.getHeader("Set-Cookie"))
                .doesNotContain("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }

    private CookieSecurityProperties secureCookieProperties() {
        CookieSecurityProperties properties = new CookieSecurityProperties();
        properties.setSecure(true);
        properties.setHttpOnly(true);
        properties.setSameSite("Lax");
        return properties;
    }

    private TransportSecurityProperties strictTransport() {
        TransportSecurityProperties properties = new TransportSecurityProperties();
        properties.setAllowLoopbackHttp(false);
        return properties;
    }

    private TransportSecurityProperties localTransport() {
        TransportSecurityProperties properties = new TransportSecurityProperties();
        properties.setAllowLoopbackHttp(true);
        return properties;
    }
}
