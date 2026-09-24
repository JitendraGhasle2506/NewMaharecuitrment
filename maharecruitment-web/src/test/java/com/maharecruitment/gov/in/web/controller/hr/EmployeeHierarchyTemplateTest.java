package com.maharecruitment.gov.in.web.controller.hr;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

class EmployeeHierarchyTemplateTest {
    @Test
    void rendersChartSectionWithContextRelativeUrls() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/hr/employee-hierarchy.html"));
        // Render this page's expressions independently from the session-dependent shared application shell.
        template = template.replaceAll("th:replace=\"[^\"]*\"", "");
        var request = new MockHttpServletRequest("GET", "/portal/hr/employee-hierarchy");
        request.setContextPath("/portal");
        var application = JakartaServletWebApplication.buildApplication(new MockServletContext());
        var context = new WebContext(application.buildExchange(request, new MockHttpServletResponse()));
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        String html = engine.process(template, context);
        assertThat(html).contains("data-api=\"/portal/api/employees/hierarchy\"", "data-context=\"/portal/\"",
                "src=\"/portal/js/employee-hierarchy.js\"", "href=\"/portal/css/employee-hierarchy.css\"",
                "id=\"ehHod\"", "id=\"ehViewport\"", "id=\"ehSearch\"",
                "aria-controls=\"ehFilterFields\"", "id=\"ehVisibleCount\"", "id=\"ehFullscreen\"",
                "id=\"ehRetry\"", "href=\"#eh-i-chart\"");
    }
}
