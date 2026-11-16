package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

class PrivacyPolicyPageTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void servesTheFaceAttendancePrivacyPolicy() throws Exception {
        String page = template("privacy-policy.html");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver() {
            @Override
            protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String owner,
                    String template, Map<String, Object> attributes) {
                return new StringTemplateResource(page);
            }
        });
        var resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(engine);
        resolver.setCharacterEncoding("UTF-8");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PrivacyPolicyController())
                .setViewResolvers(resolver)
                .build();

        String html = mvc.perform(get("/privacy-policy"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .contains("<title>Privacy Policy - MahaIT Face Attendance Application</title>")
                .contains("Effective Date: June 1, 2025 | Version 1.0.0")
                .contains("1. Information We Collect", "2. Device Permissions Required", "9. Contact Us")
                .contains("<code>NSLocationWhenInUseUsageDescription</code>")
                .contains("href=\"mailto:support@mahait.org.in\"", "href=\"mailto:support@mahaitgov.in\"")
                .contains("rel=\"noopener noreferrer\"");
    }

    @Test
    void pageIsResponsiveAndCompatibleWithTheContentSecurityPolicy() throws Exception {
        String page = template("privacy-policy.html");

        assertThat(page)
                .contains("name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"")
                .contains("overflow-wrap: anywhere")
                .contains("@media (max-width: 640px)", "content: attr(data-label)")
                // CSP blocks inline style attributes and inline scripts; the <style> block gets its nonce from Thymeleaf.
                .doesNotContain(" style=\"", "<script");
        // Every table cell carries the label shown above it when rows stack into cards on phones.
        assertThat(page.split("<td data-label=\"", -1)).hasSize(13);
    }

    @Test
    void signInPageLinksToThePolicyWhichNeedsNoLogin() throws Exception {
        assertThat(template("login.html"))
                .contains("class=\"trust-item trust-link\" th:href=\"@{/privacy-policy}\" target=\"_blank\" rel=\"noopener\"")
                .contains("Privacy Policy");

        String security = Files.readString(Path.of(
                "src/main/java/com/maharecruitment/gov/in/web/config/SecurityConfig.java"));
        int publicPages = security.indexOf("\"/\", \"/index\"");
        assertThat(publicPages).isPositive();
        String publicBlock = security.substring(publicPages, security.indexOf(".permitAll()", publicPages));
        assertThat(publicBlock).contains("\"/privacy-policy\"");
    }

    private static String template(String name) throws Exception {
        return Files.readString(TEMPLATES.resolve(name));
    }
}
