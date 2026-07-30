package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;

import com.maharecruitment.gov.in.common.security.CookieSecurityProperties;

class SessionCookieSecurityConfigTest {

    @Test
    void appliesSecureHttpOnlyAndSameSiteToContainerSessionCookie() {
        CookieSecurityProperties properties = new CookieSecurityProperties();
        properties.setSecure(true);
        properties.setHttpOnly(true);
        properties.setSameSite("Lax");
        MockServletContext servletContext = new MockServletContext();

        new SessionCookieSecurityConfig(properties).onStartup(servletContext);

        assertThat(servletContext.getSessionCookieConfig().isSecure()).isTrue();
        assertThat(servletContext.getSessionCookieConfig().isHttpOnly()).isTrue();
        assertThat(servletContext.getSessionCookieConfig().getAttribute("SameSite")).isEqualTo("Lax");
    }
}
