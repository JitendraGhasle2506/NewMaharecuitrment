package com.maharecruitment.gov.in.web.security.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

class CspTemplateHandlerTest {

    @Test
    void supportsValuelessBooleanAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        engine.addDialect(new CspTemplateDialect());

        String rendered = engine.process(
                "<script src=\"/app.js\" defer></script>",
                webContext(request, response, Map.of()));

        assertThat(rendered)
                .contains("src=\"/app.js\"")
                .contains("defer")
                .contains("nonce=\"" + SecurityHeaderPolicy.nonce(request) + "\"");
    }

    @Test
    void trustsOnlyAuthoredTemplateElementsAndFinalAttributeValues() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String untrustedHtml = "<script>alert('untrusted')</script>";
        String template = """
                <html><head><style>.hidden { display: none; }</style></head><body>
                <button style="color: red" onclick="save(&quot;42&quot;)">Save</button>
                <button th:style="|color: ${color}|" th:onclick="|save(${recordId})|">Dynamic</button>
                <div th:utext="${untrustedHtml}"></div>
                <script>function save(id) { return id; }</script>
                </body></html>
                """;

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        engine.addDialect(new CspTemplateDialect());
        WebContext context = webContext(request, response, Map.of(
                "untrustedHtml", untrustedHtml,
                "color", "blue",
                "recordId", 43));

        String rendered = engine.process(template, context);
        SecurityHeaderPolicy.writeFinalContentSecurityPolicy(request, response);
        String nonce = SecurityHeaderPolicy.nonce(request);

        assertThat(rendered)
                .contains("<style nonce=\"" + nonce + "\">")
                .contains("<script nonce=\"" + nonce + "\">function save")
                .contains("style=\"color: blue\" onclick=\"save(43)\"")
                .contains("<script>alert('untrusted')</script>")
                .doesNotContain("<script nonce=\"" + nonce + "\">alert('untrusted')");
        assertThat(response.getHeader("Content-Security-Policy"))
                .contains("'nonce-" + nonce + "'")
                .contains("script-src-attr 'unsafe-hashes'")
                .contains("style-src-attr 'unsafe-hashes'")
                .contains("'sha256-" + SecurityHeaderPolicy.sha256("save(\"42\")") + "'")
                .contains("'sha256-" + SecurityHeaderPolicy.sha256("color: red") + "'")
                .contains("'sha256-" + SecurityHeaderPolicy.sha256("save(43)") + "'")
                .contains("'sha256-" + SecurityHeaderPolicy.sha256("color: blue") + "'")
                .doesNotContain("'unsafe-inline'");
    }

    private WebContext webContext(
            MockHttpServletRequest request,
            MockHttpServletResponse response,
            Map<String, Object> variables) {
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(
                new MockServletContext());
        return new WebContext(
                application.buildExchange(request, response),
                Locale.ENGLISH,
                variables);
    }
}
