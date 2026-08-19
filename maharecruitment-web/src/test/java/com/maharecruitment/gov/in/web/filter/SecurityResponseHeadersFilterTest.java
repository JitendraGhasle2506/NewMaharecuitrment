package com.maharecruitment.gov.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.maharecruitment.gov.in.web.security.headers.SecurityHeaderPolicy;

import jakarta.servlet.http.HttpServletResponse;

class SecurityResponseHeadersFilterTest {

    private final SecurityResponseHeadersFilter filter = new SecurityResponseHeadersFilter();

    @Test
    void secureResponseContainsCompleteSecurityHeaderPolicy() throws Exception {
        MockHttpServletRequest request = request("/login", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertCompletePolicy(response, "DENY", "frame-ancestors 'none'");
        assertThat(response.getHeader("Strict-Transport-Security"))
                .contains("max-age=31536000")
                .contains("includeSubDomains");
    }

    @Test
    void insecureResponseOmitsHstsButRetainsOtherHeaders() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/login", false), response, (servletRequest, servletResponse) -> {
        });

        assertCompletePolicy(response, "DENY", "frame-ancestors 'none'");
        assertThat(response.getHeader("Strict-Transport-Security")).isNull();
    }

    @Test
    void redirectCompletedBeforeSpringSecurityStillContainsHeaders() throws Exception {
        MockHttpServletRequest request = request("/legacy", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).sendRedirect("/login"));

        assertThat(response.getStatus()).isEqualTo(302);
        assertCompletePolicy(response, "DENY", "frame-ancestors 'none'");
    }

    @Test
    void responseResetCannotRemoveSecurityHeaders() throws Exception {
        MockHttpServletRequest request = request("/error", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.reset();
            httpResponse.sendError(500);
        });

        assertThat(response.getStatus()).isEqualTo(500);
        assertCompletePolicy(response, "DENY", "frame-ancestors 'none'");
    }

    @Test
    void invoicePreviewRetainsNarrowSameOriginFramePolicy() throws Exception {
        MockHttpServletRequest request = request(
                "/maharecruitment/invoice/tax-invoices/application/42/preview/new", true);
        request.setContextPath("/maharecruitment");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertCompletePolicy(
                response,
                "SAMEORIGIN",
                "frame-ancestors 'self'");
    }

    @Test
    void filterContinuesTheApplicationChain() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request("/assets/app.css", true), new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> invoked.set(true));

        assertThat(invoked).isTrue();
    }

    @Test
    void htmlResponseIsPreservedAndUsesNoncePolicyWithoutBroadInlinePermission() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/dashboard", true), response, (servletRequest, servletResponse) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setContentType("text/html;charset=UTF-8");
            httpResponse.getWriter().write("""
                    <html><head><style>.hidden { display:none; }</style></head>
                    <body><button style="color:red" onclick="save()">Save</button>
                    <script>function save() { return true; }</script></body></html>
                    """);
        });

        String policy = response.getHeader("Content-Security-Policy");
        assertThat(response.getContentAsString())
                .contains("<style>.hidden { display:none; }</style>")
                .contains("<script>function save() { return true; }</script>");
        assertThat(policy)
                .contains("script-src 'self' 'nonce-")
                .contains("style-src 'self' 'nonce-")
                .contains("script-src-attr 'none'")
                .contains("style-src-attr 'none'")
                .doesNotContain("'unsafe-inline'");
    }

    private void assertCompletePolicy(
            MockHttpServletResponse response,
            String framePolicy,
            String frameAncestors) {
        assertThat(response.getHeader("X-XSS-Protection")).isEqualTo("0");
        assertThat(response.getHeader("Content-Security-Policy"))
                .contains("script-src 'self' 'nonce-")
                .contains("style-src 'self' 'nonce-")
                .contains("script-src-attr 'none'")
                .contains("style-src-attr 'none'")
                .contains(frameAncestors)
                .doesNotContain("'unsafe-inline'");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("Permissions-Policy")).isEqualTo(SecurityHeaderPolicy.PERMISSIONS_POLICY);
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo(framePolicy);
    }

    private MockHttpServletRequest request(String uri, boolean secure) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setSecure(secure);
        return request;
    }
}
