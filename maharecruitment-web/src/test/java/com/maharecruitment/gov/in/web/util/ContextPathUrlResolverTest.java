package com.maharecruitment.gov.in.web.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContextPathUrlResolverTest {

    private static final String CONTEXT_PATH = "/MahaitRecruitment";

    private final ContextPathUrlResolver resolver = new ContextPathUrlResolver();

    @Test
    void resolveDoesNotDuplicateContextPathWhenUrlAlreadyContainsIt() {
        assertThat(resolver.resolve(CONTEXT_PATH, "/MahaitRecruitment/"))
                .isEqualTo("/MahaitRecruitment/");
        assertThat(resolver.resolve(CONTEXT_PATH, "MahaitRecruitment/"))
                .isEqualTo("/MahaitRecruitment/");
        assertThat(resolver.resolve(CONTEXT_PATH, "/MahaitRecruitment/MahaitRecruitment/"))
                .isEqualTo("/MahaitRecruitment/");
        assertThat(resolver.resolve(CONTEXT_PATH, "/MahaitRecruitment/admin/dashboard"))
                .isEqualTo("/MahaitRecruitment/admin/dashboard");
    }

    @Test
    void resolveAddsContextPathForApplicationRelativeUrl() {
        assertThat(resolver.resolve(CONTEXT_PATH, "/login"))
                .isEqualTo("/MahaitRecruitment/login");
        assertThat(resolver.resolve(CONTEXT_PATH, "login"))
                .isEqualTo("/MahaitRecruitment/login");
    }

    @Test
    void toRedirectPathRemovesContextPathForSpringRedirectView() {
        assertThat(resolver.toRedirectPath(CONTEXT_PATH, "/MahaitRecruitment/admin/dashboard", "/common"))
                .isEqualTo("/admin/dashboard");
        assertThat(resolver.toRedirectPath(CONTEXT_PATH, "MahaitRecruitment/", "/common"))
                .isEqualTo("/");
        assertThat(resolver.toRedirectPath(CONTEXT_PATH, "/admin/dashboard", "/common"))
                .isEqualTo("/admin/dashboard");
    }
}
