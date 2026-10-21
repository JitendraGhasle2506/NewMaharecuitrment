package com.maharecruitment.gov.in.web.controller.hr;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.service.hr.EmployeeExitService;
import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeOnboardingDetailView;

class EmployeeExitTemplateTest {
    @Test void rendersResponsiveFormAndValidationErrorsUsingActualThymeleafTemplate() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/templates/hr/employee-exit.html"))
                .replaceAll("th:replace=\"[^\"]*\"", "");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver() {
            @Override protected ITemplateResource computeTemplateResource(IEngineConfiguration config,
                    String owner, String template, Map<String, Object> attributes) {
                return new StringTemplateResource(source);
            }
        });
        var servlet = new MockServletContext();
        try (var app = new StaticWebApplicationContext()) {
            app.setServletContext(servlet);
            app.refresh();
            var resolver = new ThymeleafViewResolver();

            resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            var onboarding = mock(HROnboardingPageService.class);
            var details = new AgencyPreOnboardingForm();
            details.setName("Test <Employee>");
            when(onboarding.loadEmployeeDetail(5L)).thenReturn(new EmployeeOnboardingDetailView(
                    5L, "EMP005", "ACTIVE", "MAHAIT", null, details));
            var service = mock(EmployeeExitService.class);
            var mvc = MockMvcBuilders.standaloneSetup(new EmployeeExitController(service, onboarding))
                    .setViewResolvers(resolver).build();
            String html = mvc.perform(get("/portal/hr/employees/5/exit").contextPath("/portal"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("Test &lt;Employee&gt;", "action=\"/portal/hr/employees/5/exit\"",
                    "name=\"lastWorkingDate\"", "name=\"confirmed\"", "employee-exit.css", "maxlength=\"1000\"")
                    .doesNotContain("th:field", "Test <Employee>");
            Path fixture = Path.of("../artifacts/hr-employee-exit-preview.html");
            Files.createDirectories(fixture.getParent());
            Files.writeString(fixture, html);
            String errors = mvc.perform(post("/hr/employees/5/exit").principal(() -> "hr@example.test"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(errors).contains("Enter the last working date.", "Enter a reason for the exit.",
                    "Confirm that the employee has left", "is-invalid");
            verifyNoInteractions(service);
        }
    }
}