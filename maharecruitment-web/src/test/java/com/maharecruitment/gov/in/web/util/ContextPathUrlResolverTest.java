package com.maharecruitment.gov.in.web.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContextPathUrlResolverTest {

    private static final String CONTEXT_PATH = "/maharecruitment";

    private final ContextPathUrlResolver resolver = new ContextPathUrlResolver();

    @Test
    void resolveDoesNotDuplicateContextPathWhenUrlAlreadyContainsIt() {
        assertThat(resolver.resolve(CONTEXT_PATH, "/maharecruitment/"))
                .isEqualTo("/maharecruitment/");
        assertThat(resolver.resolve(CONTEXT_PATH, "maharecruitment/"))
                .isEqualTo("/maharecruitment/");
        assertThat(resolver.resolve(CONTEXT_PATH, "/maharecruitment/maharecruitment/"))
                .isEqualTo("/maharecruitment/");
        assertThat(resolver.resolve(CONTEXT_PATH, "/maharecruitment/admin/dashboard"))
                .isEqualTo("/maharecruitment/admin/dashboard");
    }

    @Test
    void resolveAddsContextPathForApplicationRelativeUrl() {
        assertThat(resolver.resolve(CONTEXT_PATH, "/login"))
                .isEqualTo("/maharecruitment/login");
        assertThat(resolver.resolve(CONTEXT_PATH, "login"))
                .isEqualTo("/maharecruitment/login");
    }

    @Test
    void toRedirectPathRemovesContextPathForSpringRedirectView() {
        assertThat(resolver.toRedirectPath(CONTEXT_PATH, "/maharecruitment/admin/dashboard", "/common"))
                .isEqualTo("/admin/dashboard");
        assertThat(resolver.toRedirectPath(CONTEXT_PATH, "maharecruitment/", "/common"))
                .isEqualTo("/");
        assertThat(resolver.toRedirectPath(CONTEXT_PATH, "/admin/dashboard", "/common"))
                .isEqualTo("/admin/dashboard");
    }
}
