package com.maharecruitment.gov.in.web.security.headers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SecurityHeaderPolicyTest {

    @Test
    void allowsOnlyKnownInvoiceFrameResponses() {
        assertThat(allows("/invoice/tax-invoices/application/42/new")).isTrue();
        assertThat(allows("/invoice/tax-invoices/application/42/preview")).isTrue();
        assertThat(allows("/invoice/tax-invoices/application/42/preview/new")).isTrue();
        assertThat(allows("/invoice/tax-invoices/application/42/preview/old")).isTrue();

        assertThat(allows("/invoice/tax-invoices/application/42")).isFalse();
        assertThat(allows("/invoice/tax-invoices/42")).isFalse();
        assertThat(allows("/login")).isFalse();
    }

    @Test
    void matcherRemovesApplicationContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/maharecruitment");
        request.setRequestURI("/maharecruitment/invoice/tax-invoices/application/42/preview/new");

        assertThat(SecurityHeaderPolicy.allowsSameOriginFraming(request)).isTrue();
    }

    private boolean allows(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        return SecurityHeaderPolicy.allowsSameOriginFraming(request);
    }
}
